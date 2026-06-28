package com.everypicfound.vectorization.domain.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.ibatis.builder.ResultMapResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.everypicfound.common.cache.CacheKeyBuilder;
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
1. 判断文本向量缓存是否开启:get 命中后检查 cachedEmbedding.dim == vectorDim, put 前检查 embedding.dim == vectorDim
2. 生成文本向量缓存 key
3. 从 CacheService 读取 QueryEmbedding
4. 把 QueryEmbedding 写入缓存
5. 删除某个文本向量缓存
*/

@Service
@RequiredArgsConstructor
public class DefaultTextVectorCacheService implements TextVectorCacheService {

    private static final String CACHE_NAME = "text_vector";

    private static final String OPERATION_GET = "get";
    private static final String OPERATION_PUT = "put";
    private static final String OPERATION_EVICT = "evict";

    private static final String RESULT_HIT = "hit";
    private static final String RESULT_MISS = "miss";
    private static final String RESULT_INVALID = "invalid";
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_ERROR = "error";
    private static final String RESULT_SKIPPED = "skipped";

    private final LogService logService;

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final long DEFAULT_TEXT_VECTOR_CACHE_TTL_SECONDS = 3600L;

    private final CacheService cacheService;

    private final CacheKeyBuilder cacheKeyBuilder;

    private final VectorCacheProperties vectorCacheProperties;

    private final MetricRecorder metricRecorder;

    @Override
    public QueryEmbedding get(String modelName,
            Integer vectorDim,
            String queryText) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {

            if (!isCacheableTextVector(modelName, vectorDim, queryText)) {
                return null;
            }

            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            QueryEmbedding cachedEmbedding = cacheService.get(cacheKey, QueryEmbedding.class);
            if (cachedEmbedding == null) {
                result = RESULT_MISS;
                return null;
            }

            if (!isValidCachedEmbedding(cachedEmbedding, vectorDim)) {
                result = RESULT_INVALID;
                // 非法缓存值不继续使用，并尝试清理
                evictInvalidCacheValue(cacheKey);
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
    public void put(String modelName,
            Integer vectorDim,
            String queryText,
            QueryEmbedding embedding) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {
            if (!isCacheableTextVector(modelName, vectorDim, queryText)) {
                return;
            }
            if (!isValidCachedEmbedding(embedding, vectorDim)) {
                return;
            }

            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            cacheService.put(cacheKey, embedding, getTextVectorCacheTtl());
            result = RESULT_SUCCESS;
        } catch (RuntimeException e) {
            result = RESULT_ERROR;
            recordCacheFailure(OPERATION_PUT, e);
        } finally {
            recordCacheMetrics(OPERATION_PUT, result, startNanos);
        }
    }

    @Override
    public void evict(String modelName,
            Integer vectorDim,
            String queryText) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;
        
        try {
            if (!isValidTextVectorKey(modelName, vectorDim, queryText)) {
                return;
            }

            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            cacheService.evict(cacheKey);
            result = RESULT_SUCCESS;
        } catch (RuntimeException e) {
            result = RESULT_ERROR;
            recordCacheFailure(OPERATION_EVICT, e);
        } finally {
            recordCacheMetrics(OPERATION_EVICT, result, startNanos);
        }
    }

    private boolean isCacheableTextVector(String modelName,
            Integer vectorDim,
            String queryText) {
        return Boolean.TRUE.equals(cacheService.isEnabled())
                && Boolean.TRUE.equals(vectorCacheProperties.getEnabled())
                && isValidTextVectorKey(modelName, vectorDim, queryText);
    }

    private boolean isValidTextVectorKey(String modelName,
            Integer vectorDim,
            String queryText) {
        return StringUtils.hasText(modelName)
                && vectorDim != null
                && vectorDim > 0
                && StringUtils.hasText(queryText);
    }

    private boolean isValidCachedEmbedding(QueryEmbedding embedding, Integer vectorDim) {
        return embedding != null
                && embedding.getEmbedding() != null
                && !embedding.getEmbedding().isEmpty()
                && embedding.getDim() != null
                && embedding.getDim().equals(vectorDim);
    }

    private String buildCacheKey(String modelName,
            Integer vectorDim,
            String queryText) {
        String rawKey = String.join(":",
                modelName,
                String.valueOf(vectorDim),
                hashText(queryText));

        return cacheKeyBuilder.buildVectorKey(rawKey);
    }

    private Duration getTextVectorCacheTtl() {
        Long ttlSeconds = vectorCacheProperties.getTextVectorTtlSeconds();
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Duration.ofSeconds(DEFAULT_TEXT_VECTOR_CACHE_TTL_SECONDS);
        }
        return Duration.ofSeconds(ttlSeconds);
    }

    private String hashText(String text) {
        String normalizedText = text.trim();

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(normalizedText.getBytes(StandardCharsets.UTF_8));
            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " algorithm is not available", e);
        }
    }

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

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);// 1 byte 转化为 16 进制为 2 位，长度翻倍
        for (byte currentByte : bytes) {
            String value = Integer.toHexString(currentByte & 0xff);// 把 本来存在复数的 byte 转成 0 到 255 的正整数。
            if (value.length() == 1) {
                hex.append('0');
            } // 需要对单位数开头补零，确保转化为两位
            hex.append(value);
        }
        return hex.toString();
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
                .errorCode("TEXT_VECTOR_CACHE_FAILED")
                .message(
                        "text vector cache operation failed")
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
                                "TEXT_VECTOR_CACHE_FAILED")
                        .message(
                                "vectorization continues without text vector cache")
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
