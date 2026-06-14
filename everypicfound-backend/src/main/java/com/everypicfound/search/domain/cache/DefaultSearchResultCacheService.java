package com.everypicfound.search.domain.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.everypicfound.common.cache.CacheKeyBuilder;
import com.everypicfound.common.cache.CacheService;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.collection.SearchCollectionContext;
import com.everypicfound.search.domain.enums.SearchType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchResultCacheService implements SearchResultCacheService{

    private static final Logger log = LoggerFactory.getLogger(DefaultSearchResultCacheService.class);

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final long DEFAULT_RESULT_CACHE_TTL_SECONDS = 300L;

    private final CacheService cacheService;//真正读写缓存

    private final CacheKeyBuilder cacheKeyBuilder;//生成统一格式的最终缓存 key


    private final SearchProperties searchProperties;//读取搜索缓存开关、TTL、是否缓存空结果

    //如果命中缓存，就返回 SearchResponse。如果不该缓存、或者没命中，就返回 null。
    @Override
    public SearchResponse get(SearchCommand command,
                            SearchCollectionContext collectionContext,
                            Integer topK) {
        if (!isCacheableTextSearch(command, collectionContext, topK)) {
            return null;
        }

        try {
            String cacheKey = buildCacheKey(command.getQueryText(), collectionContext, topK);
            return cacheService.get(cacheKey, SearchResponse.class);
        } catch (RuntimeException e) {
            log.warn("Search result cache get failed.", e);
            //缓存是旁路优化，搜索是主流程。旁路失败，只记录日志；主流程继续
            return null;
        }
    }


    //如果缓存不存在，就写入缓存。
    @Override
    public void put(SearchCommand command,
                    SearchCollectionContext collectionContext,
                    Integer topK,
                    SearchResponse response) {
        if (!isCacheableTextSearch(command, collectionContext, topK)) {
            return;
        }
        if (!shouldCacheResponse(response)) {
            return;
        }

        try {
            String cacheKey = buildCacheKey(command.getQueryText(), collectionContext, topK);
            Duration ttl = getResultCacheTtl();
            cacheService.put(cacheKey, response, ttl);
        } catch (RuntimeException e) {
            log.warn("Search result cache put failed.", e);
            //缓存是旁路优化，搜索是主流程。旁路失败，只记录日志；主流程继续
        }
    }

    //删除缓存
    @Override
    public void evictByText(String queryText,
                            SearchCollectionContext collectionContext,
                            Integer topK) {
        if (!StringUtils.hasText(queryText)
                || !isValidCollectionContext(collectionContext)
                || topK == null
                || topK <= 0) {
            return;
        }

        try {
            String cacheKey = buildCacheKey(queryText, collectionContext, topK);
            cacheService.evict(cacheKey);
        } catch (RuntimeException e) {
            log.warn("Search result cache evict failed.", e);
        }
    }

    private boolean isCacheableTextSearch(SearchCommand command,
                                        SearchCollectionContext collectionContext,
                                        Integer topK) {
        return Boolean.TRUE.equals(cacheService.isEnabled())
                &&Boolean.TRUE.equals(searchProperties.getCacheEnabled())
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

    //把搜索词转化为hash字符串
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


    //把hash的二进制结果转成16进制字符串,MessageDigest 算出来的是 byte[]，不适合直接放进 key。这个函数帮助转换
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
    }//hashText 和 toHex 这两个函数的组合，能把任意长度的搜索词转化为固定长度的字符串（SHA-256 的输出是 64 个字符的十六进制字符串）。这样既保证了 key 的长度可控，又能有效区分不同的搜索词，减少缓存冲突的概率。

}


