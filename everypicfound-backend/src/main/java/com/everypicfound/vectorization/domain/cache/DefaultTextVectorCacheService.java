package com.everypicfound.vectorization.domain.cache;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.everypicfound.common.cache.CacheKeyBuilder;
import com.everypicfound.common.cache.CacheService;
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
    private static final Logger log = LoggerFactory.getLogger(DefaultTextVectorCacheService.class);

    private static final String HASH_ALGORITHM = "SHA-256";

    private static final long DEFAULT_TEXT_VECTOR_CACHE_TTL_SECONDS = 3600L;

    private final CacheService cacheService;

    private final CacheKeyBuilder cacheKeyBuilder;

    private final VectorCacheProperties vectorCacheProperties;

        @Override
    public QueryEmbedding get(String modelName,
                            Integer vectorDim,
                            String queryText) {
        if (!isCacheableTextVector(modelName, vectorDim, queryText)) {
            return null;
        }

        try {
            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            QueryEmbedding cachedEmbedding = cacheService.get(cacheKey, QueryEmbedding.class);
            if (!isValidCachedEmbedding(cachedEmbedding, vectorDim)) {
                return null;
            }
            return cachedEmbedding;
        } catch (RuntimeException e) {
            log.warn("Text vector cache get failed.", e);
            return null;
        }
    }

        @Override
    public void put(String modelName,
                    Integer vectorDim,
                    String queryText,
                    QueryEmbedding embedding) {
        if (!isCacheableTextVector(modelName, vectorDim, queryText)) {
            return;
        }
        if (!isValidCachedEmbedding(embedding, vectorDim)) {
            return;
        }

        try {
            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            cacheService.put(cacheKey, embedding, getTextVectorCacheTtl());
        } catch (RuntimeException e) {
            log.warn("Text vector cache put failed.", e);
        }
    }

        @Override
    public void evict(String modelName,
                    Integer vectorDim,
                    String queryText) {
        if (!isValidTextVectorKey(modelName, vectorDim, queryText)) {
            return;
        }

        try {
            String cacheKey = buildCacheKey(modelName, vectorDim, queryText);
            cacheService.evict(cacheKey);
        } catch (RuntimeException e) {
            log.warn("Text vector cache evict failed.", e);
        }
    }

        private boolean isCacheableTextVector(String modelName,
                                        Integer vectorDim,
                                        String queryText) {
            return Boolean.TRUE.equals(cacheService.isEnabled()) 
                &&Boolean.TRUE.equals(vectorCacheProperties.getEnabled())
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

    private String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);//1 byte 转化为 16 进制为 2 位，长度翻倍
        for (byte currentByte : bytes) {
            String value = Integer.toHexString(currentByte & 0xff);//把 本来存在复数的 byte 转成 0 到 255 的正整数。
            if (value.length() == 1) {
                hex.append('0');
            }//需要对单位数开头补零，确保转化为两位
            hex.append(value);
        }
        return hex.toString();
    }

}



