package com.everypicfound.vectorization.domain.query;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.stereotype.Component;

import com.everypicfound.common.cache.CacheKeyBuilder;
import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorization.domain.cache.QueryVectorCacheService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TextQueryVectorizer implements QueryVectorizer {

    private static final String HASH_ALGORITHM = "SHA-256";

    private final ModelVectorizationClient modelVectorizationClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final QueryVectorCacheService queryVectorCacheService;// 新加缓存服务

    private final CacheKeyBuilder cacheKeyBuilder;

    @Override
    public SearchType supportType() {
        return SearchType.TEXT;
    }

    /**
     * 文本查询向量化流程：
     *
     * 1. 校验文本查询参数；
     * 2. 获取当前活动集合配置；
     * 3. 生成文本查询向量缓存 Key；
     * 4. 查询缓存；
     * 5. 缓存未命中时调用模型服务；
     * 6. 写入查询向量缓存；
     * 7. 返回查询向量。
     */
    @Override
    public QueryEmbedding vectorize(QueryVectorizeRequest request) {

        if (request == null || request.getQueryText() == null || request.getQueryText().isBlank()) {
            throw new BizException(SearchErrorCode.SEARCH_TEXT_EMPTY);
        }

        VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();
        String queryText = request.getQueryText().trim();

        String key = buildCacheKey(config, queryText);

        QueryEmbedding cachedEmbedding = queryVectorCacheService.get(key, config.getVectorDim());
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }

        TextVectorizeRequest textRequest = TextVectorizeRequest.builder()
                .text(queryText)
                .modelName(config.getModelName())
                .traceId(request.getTraceId())
                .requestId(request.getRequestId())
                .build();

        VectorizeResult result = modelVectorizationClient.vectorizeText(textRequest);

        QueryEmbedding queryEmbedding = buildQueryEmbedding(SearchType.TEXT, result);

        queryVectorCacheService.put(
                key,
                config.getVectorDim(),
                queryEmbedding     
        );

        return queryEmbedding;
    }

    private QueryEmbedding buildQueryEmbedding(SearchType searchType, VectorizeResult result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            throw new SystemException(SearchErrorCode.QUERY_VECTORIZATION_FAILED);
        }
        if (result.getEmbedding() == null || result.getEmbedding().isEmpty()) {
            throw new SystemException(SearchErrorCode.QUERY_EMBEDDING_EMPTY);
        }

        return QueryEmbedding.builder()
                .searchType(searchType)
                .embedding(result.getEmbedding())
                .dim(result.getDim())
                .modelName(result.getModelName())
                .build();
    }

    /**
     * 生成文本查询向量缓存 Key。
     *
     * Key 业务组成：
     * 搜索类型 + 模型名称 + 向量版本 + 向量维度 + 文本摘要。
     */
    private String buildCacheKey(
            VectorCollectionConfig config,
            String queryText) {
        String rawKey = String.join(
                ":",
                SearchType.TEXT.name(),
                config.getModelName(),
                config.getVectorVersion(),
                String.valueOf(config.getVectorDim()),
                hashText(queryText));
        return cacheKeyBuilder.buildQueryVectorKey(rawKey);       
    }

    /**
     * 对规范化后的查询文本计算 SHA-256 摘要。
     *
     * 这里只清理首尾空格，不转换大小写，
     * 避免修改模型实际接收文本的语义。
     */
    private String hashText(String text) {
        String normalizedText = text.trim();

        try {
            MessageDigest digest =
                    MessageDigest.getInstance(
                            HASH_ALGORITHM);

            byte[] hashBytes = digest.digest(
                    normalizedText.getBytes(
                            StandardCharsets.UTF_8));

            return toHex(hashBytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    HASH_ALGORITHM
                            + " algorithm is not available",
                    exception);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder result =
                new StringBuilder(bytes.length * 2);

        for (byte currentByte : bytes) {
            String hexValue = Integer.toHexString(
                    currentByte & 0xff);

            if (hexValue.length() == 1) {
                result.append('0');
            }

            result.append(hexValue);
        }

        return result.toString();
    }

}
