package com.everypicfound.search.domain.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.everypicfound.search.observability.SearchObservationContext;
import com.everypicfound.search.observability.SearchObservationContext.CacheLookupStatus;
import com.everypicfound.search.observability.SearchObservationContext.CacheWriteStatus;

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
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.collection.SearchCollectionContext;
import com.everypicfound.search.domain.enums.SearchType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchResultCacheService implements SearchResultCacheService {

    private static final String CACHE_NAME = "search_result";

    private static final String OPERATION_GET = "get";
    private static final String OPERATION_PUT = "put";
    private static final String OPERATION_EVICT = "evict";

    private static final String RESULT_HIT = "hit";
    private static final String RESULT_MISS = "miss";
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_ERROR = "error";
    private static final String RESULT_SKIPPED = "skipped";

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final long DEFAULT_RESULT_CACHE_TTL_SECONDS = 300L;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    private final CacheService cacheService;// 真正读写缓存

    private final CacheKeyBuilder cacheKeyBuilder;// 生成统一格式的最终缓存 key

    private final SearchProperties searchProperties;// 读取搜索缓存开关、TTL、是否缓存空结果

    // 如果命中缓存，就返回 SearchResponse。如果不该缓存、或者没命中，就返回 null。
    @Override
    public SearchResponse get(SearchCommand command,
            SearchCollectionContext collectionContext,
            Integer topK) {

        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {
            if (!isCacheableTextSearch(command, collectionContext, topK)) {
                SearchObservationContext.markCacheLookup(CacheLookupStatus.NOT_APPLICABLE);
                return null;
            }

            String cacheKey = buildCacheKey(command.getQueryText(), collectionContext, topK);
            SearchResponse response = cacheService.get(cacheKey, SearchResponse.class);

            if (response == null) {
                result = RESULT_MISS;
                SearchObservationContext.markCacheLookup(
                        CacheLookupStatus.MISS);
                return null;
            }

            result = RESULT_HIT;

            SearchObservationContext.markCacheLookup(response == null ? CacheLookupStatus.MISS : CacheLookupStatus.HIT);

            return response;
        } catch (RuntimeException exception) {
            result = RESULT_ERROR;

            SearchObservationContext.markCacheLookup(CacheLookupStatus.ERROR);
            recordCacheFailure(OPERATION_GET, exception);
            // 缓存是旁路优化，搜索是主流程。旁路失败，只记录日志；主流程继续
            return null;
        } finally {
            recordCacheMetrics(OPERATION_GET, result, startNanos);
        }
    }

    // 如果缓存不存在，就写入缓存。
    @Override
    public void put(SearchCommand command,
            SearchCollectionContext collectionContext,
            Integer topK,
            SearchResponse response) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;

        try {

            if (!isCacheableTextSearch(command, collectionContext, topK) || !shouldCacheResponse(response)) {

                SearchObservationContext.markCacheWrite(CacheWriteStatus.SKIPPED);
                return;
            }

            String cacheKey = buildCacheKey(command.getQueryText(), collectionContext, topK);
            cacheService.put(cacheKey, response, getResultCacheTtl());
            result = RESULT_SUCCESS;

            SearchObservationContext.markCacheWrite(CacheWriteStatus.SUCCESS);
        } catch (RuntimeException e) {
            result = RESULT_ERROR;

            SearchObservationContext.markCacheWrite(CacheWriteStatus.ERROR);
            recordCacheFailure(OPERATION_PUT, e);
            // 缓存是旁路优化，搜索是主流程。旁路失败，只记录日志；主流程继续
        } finally {
            recordCacheMetrics(OPERATION_PUT, result, startNanos);
        }
    }

    // 删除缓存
    @Override
    public void evictByText(String queryText,
            SearchCollectionContext collectionContext,
            Integer topK) {
        long startNanos = System.nanoTime();
        String result = RESULT_SKIPPED;
        
        try {
            if (!StringUtils.hasText(queryText)
                    || !isValidCollectionContext(collectionContext)
                    || topK == null
                    || topK <= 0) {
                SearchObservationContext.markCacheLookup(CacheLookupStatus.NOT_APPLICABLE);

                return;
            }

            String cacheKey = buildCacheKey(queryText, collectionContext, topK);
            cacheService.evict(cacheKey);
            result = RESULT_SUCCESS;
        } catch (RuntimeException e) {
            result = RESULT_ERROR;

            SearchObservationContext.markCacheLookup(CacheLookupStatus.ERROR);
            recordCacheFailure("evict", e);
        } finally {
            recordCacheMetrics(OPERATION_EVICT, result, startNanos);
        }
    }

    private boolean isCacheableTextSearch(SearchCommand command,
            SearchCollectionContext collectionContext,
            Integer topK) {
        return Boolean.TRUE.equals(cacheService.isEnabled())
                && Boolean.TRUE.equals(searchProperties.getCacheEnabled())
                && command != null
                && command.getSearchType() == SearchType.TEXT
                && StringUtils.hasText(command.getQueryText())
                && isValidCollectionContext(collectionContext)
                && topK != null
                && topK > 0;
    }

    private boolean isValidCollectionContext(SearchCollectionContext collectionContext) {
        return collectionContext != null
                && StringUtils.hasText(collectionContext.getCollectionName())
                && StringUtils.hasText(collectionContext.getModelName())
                && StringUtils.hasText(collectionContext.getVectorVersion());
    }

    private boolean shouldCacheResponse(SearchResponse response) {
        if (response == null) {
            return false;
        }

        List<SearchResultItem> items = response.getItems();
        boolean emptyResult = items == null || items.isEmpty();
        return !emptyResult || Boolean.TRUE.equals(searchProperties.getCacheEmptyResult());
    }

    private String buildCacheKey(String queryText,
            SearchCollectionContext collectionContext,
            Integer topK) {
        String rawKey = String.join(":",
                SearchType.TEXT.name(),
                collectionContext.getCollectionName(),
                collectionContext.getModelName(),
                collectionContext.getVectorVersion(),
                String.valueOf(topK),
                hashText(queryText));

        return cacheKeyBuilder.buildSearchKey(rawKey);
    }

    private Duration getResultCacheTtl() {
        Long ttlSeconds = searchProperties.getResultCacheTtlSeconds();
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return Duration.ofSeconds(DEFAULT_RESULT_CACHE_TTL_SECONDS);
        }
        return Duration.ofSeconds(ttlSeconds);
    }

    // 把搜索词转化为hash字符串
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

    // 把hash的二进制结果转成16进制字符串,MessageDigest 算出来的是 byte[]，不适合直接放进 key。这个函数帮助转换
    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte currentByte : bytes) {
            String value = Integer.toHexString(currentByte & 0xff);
            if (value.length() == 1) {
                hex.append('0');
            }
            hex.append(value);
        }
        return hex.toString();
    }// hashText 和 toHex 这两个函数的组合，能把任意长度的搜索词转化为固定长度的字符串（SHA-256 的输出是 64
     // 个字符的十六进制字符串）。这样既保证了 key 的长度可控，又能有效区分不同的搜索词，减少缓存冲突的概率。

    private void recordCacheFailure(
            String operation,
            RuntimeException exception) {

        LogContext errorContext = LogContext.builder()
                .module("search")
                .bizType("CACHE")
                .operation(operation)
                .eventName(
                        LogEventName.SYSTEM_EXCEPTION_OCCURRED)
                .status(LogStatus.FAILED)
                .errorCode("SEARCH_RESULT_CACHE_FAILED")
                .message(
                        "search result cache operation failed")
                .build();

        // 异常被当前 Wrapper 吞掉，因此这里是最终 Throwable 责任点
        logService.recordError(
                errorContext,
                exception);

        logService.recordEvent(
                LogContext.builder()
                        .module("search")
                        .bizType("CACHE")
                        .operation(operation)
                        .eventName(
                                LogEventName.CACHE_DEGRADED)
                        .status(LogStatus.DEGRADED)
                        .errorCode(
                                "SEARCH_RESULT_CACHE_FAILED")
                        .message(
                                "search continues without result cache")
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
