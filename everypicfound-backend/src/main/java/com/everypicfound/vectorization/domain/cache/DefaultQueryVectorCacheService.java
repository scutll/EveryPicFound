package com.everypicfound.vectorization.domain.cache;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.everypicfound.common.cache.CacheService;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.vectorization.config.VectorCacheProperties;
import com.everypicfound.vectorization.domain.query.QueryEmbedding;

import lombok.RequiredArgsConstructor;

/*
 * 查询向量缓存通用实现：
 *
 * 1. 判断查询向量缓存是否开启；
 * 2. 根据调用方传入的完整 Key 读取 QueryEmbedding；
 * 3. 校验缓存向量维度及实际向量长度；
 * 4. 写入或删除查询向量缓存；
 * 5. 缓存异常时记录日志并降级，不影响向量化主流程。
 */

@Service
@RequiredArgsConstructor
public class DefaultQueryVectorCacheService implements QueryVectorCacheService {

    /**
     * 逻辑缓存名称。
     *
     * 与底层 Redis 指标区分：
     * - query_vector：查询向量缓存的业务指标；
     * - redis：底层缓存组件的访问指标。
     */
    private static final String CACHE_NAME = "query_vector";

    private static final String OPERATION_GET = "get";
    private static final String OPERATION_PUT = "put";
    private static final String OPERATION_EVICT = "evict";

    private static final String RESULT_HIT = "hit";
    private static final String RESULT_MISS = "miss";
    private static final String RESULT_INVALID = "invalid";
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_ERROR = "error";
    private static final String RESULT_SKIPPED = "skipped";
    
    private static final String ERROR_CODE = "QUERY_VECTOR_CACHE_FAILED";
    
    private final LogService logService;

    private static final long DEFAULT_QUERY_VECTOR_CACHE_TTL_SECONDS = 3600L;

    private final CacheService cacheService;

    private final VectorCacheProperties vectorCacheProperties;

    private final MetricRecorder metricRecorder;

    @Override
    public QueryEmbedding get(
            String key,
            Integer vectorDim) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {

            if (!isCacheable(key, vectorDim)) {
                return null;
            }

            QueryEmbedding cachedEmbedding = cacheService.get(key, QueryEmbedding.class);
            if (cachedEmbedding == null) {
                result = RESULT_MISS;
                return null;
            }

            if (!isValidEmbedding(cachedEmbedding, vectorDim)) {
                result = RESULT_INVALID;
                /*
                 * 缓存值已经存在，但内容不符合当前查询要求。
                 * 不能继续使用，并尝试清除损坏或过期的数据。
                 */
                evictInvalidCacheValue(key);
                return null;
            }

            result = RESULT_HIT;
            return cachedEmbedding;
        } catch (RuntimeException exception) {
            result = RESULT_ERROR;
            recordCacheFailure(OPERATION_GET, exception);
            return null;
        } finally {
            recordCacheMetrics(OPERATION_GET, result, startNanos);
        }
    }

    @Override
    public void put(
            String key,
            Integer vectorDim,
            QueryEmbedding embedding) {

        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {
            if (!isCacheable(
                    key,
                    vectorDim)) {

                return;
            }

            if (!isValidEmbedding(
                    embedding,
                    vectorDim)) {

                result = RESULT_INVALID;
                return;
            }

            cacheService.put(
                    key,
                    embedding,
                    getQueryVectorCacheTtl());

            result = RESULT_SUCCESS;
        } catch (RuntimeException exception) {
            /*
             * 写缓存失败不能影响本次已经完成的向量化结果。
             */
            result = RESULT_ERROR;

            recordCacheFailure(
                    OPERATION_PUT,
                    exception);
        } finally {
            recordCacheMetrics(
                    OPERATION_PUT,
                    result,
                    startNanos);
        }
    }

    @Override
    public void evict(
            String key) {

        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {
            /*
             * evict 不依赖 vectorDim，因为删除只需要准确的 Key。
             *
             * 这里也不判断业务缓存开关，使显式清理操作在缓存策略关闭时
             * 仍可尝试清除以前遗留的数据。
             */
            if (!isValidCacheIdentity(
                    key)) {

                return;
            }

            cacheService.evict(key);
            result = RESULT_SUCCESS;
        } catch (RuntimeException exception) {
            result = RESULT_ERROR;

            recordCacheFailure(
                    OPERATION_EVICT,
                    exception);
        } finally {
            recordCacheMetrics(
                    OPERATION_EVICT,
                    result,
                    startNanos);
        }
    }

    private boolean isCacheable(
            String key,
            Integer vectorDim) {
        return Boolean.TRUE.equals(cacheService.isEnabled())
                && Boolean.TRUE.equals(vectorCacheProperties.getEnabled())
                && isValidCacheIdentity(key)
                && vectorDim != null
                && vectorDim > 0;
    }
    

    /**
     * 校验定位缓存所需的基本信息。
     */
    private boolean isValidCacheIdentity(
            String key) {
        return StringUtils.hasText(key);
    }


    /**
     * 校验查询向量缓存值。
     *
     * 除了校验 dim 元数据，还需要校验实际向量长度，
     * 防止出现 dim=512、实际只有 500 个元素的损坏数据。
     */
    private boolean isValidEmbedding(QueryEmbedding embedding, Integer vectorDim) {
        return embedding != null
                && embedding.getEmbedding() != null
                && !embedding.getEmbedding().isEmpty()
                && embedding.getDim() != null
                && embedding.getDim().equals(vectorDim)
                && embedding.getEmbedding().size() == vectorDim
                && StringUtils.hasText(
                        embedding.getModelName());
    }

    private Duration getQueryVectorCacheTtl() {
        Long ttlSeconds = vectorCacheProperties.getTextVectorTtlSeconds();
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Duration.ofSeconds(DEFAULT_QUERY_VECTOR_CACHE_TTL_SECONDS);
        }
        return Duration.ofSeconds(ttlSeconds);
    }

    /**
     * 清理非法缓存。
     *
     * 清理失败只记录日志和指标，仍按照缓存未命中处理。
     */
    private void evictInvalidCacheValue(
            String cacheKey) {

        long startNanos = System.nanoTime();
        String result = RESULT_ERROR;

        try {
            cacheService.evict(cacheKey);
            result = RESULT_SUCCESS;
        } catch (RuntimeException exception) {
            recordCacheFailure(
                    OPERATION_EVICT,
                    exception);
        } finally {
            recordCacheMetrics(
                    OPERATION_EVICT,
                    result,
                    startNanos);
        }
    }


    private void recordCacheFailure(
            String operation,
            RuntimeException exception) {



        LogContext errorContext = LogContext.builder()
                .module("vectorization")
                .bizType("CACHE")
                .operation(operation)
                .eventName(
                        LogEventName.SYSTEM_EXCEPTION_OCCURRED)
                .status(LogStatus.FAILED)
                .errorCode(ERROR_CODE)
                .message(
                    "query vector cache operation failed")
                .build();

        logService.recordError(
                errorContext,
                exception);

        logService.recordEvent(
                LogContext.builder()
                        .module("vectorization")
                        .bizType("CACHE")
                        .operation(operation)
                        .eventName(
                                LogEventName.CACHE_DEGRADED)
                        .status(LogStatus.DEGRADED)
                        .errorCode(
                                ERROR_CODE)
                        .message(
                                "vectorization continues without query vector cache")
                        .build());
    }

    private void recordCacheMetrics(
            String operation,
            String result,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(
                        MetricTag.CACHE_NAME,
                        CACHE_NAME)
                .tag(
                        MetricTag.OPERATION,
                        operation)
                .tag(
                        MetricTag.RESULT,
                        result)
                .build();

        metricRecorder.increment(
                MetricName.CACHE_OPERATIONS,
                tags);

        metricRecorder.recordTimer(
                MetricName.CACHE_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

}
