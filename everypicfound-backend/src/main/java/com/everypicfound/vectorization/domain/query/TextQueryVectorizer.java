package com.everypicfound.vectorization.domain.query;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorization.domain.cache.TextVectorCacheService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TextQueryVectorizer implements QueryVectorizer {
    
    private final ModelVectorizationClient modelVectorizationClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final TextVectorCacheService textVectorCacheService;// 新加缓存服务

    @Override
    public SearchType supportType() {
        return SearchType.TEXT;
    }

    /**
     * 1. 获取当前活动的向量集合配置
     * 2. 从缓存中获取查询向量
     * 3. 调用模型向量化服务获取查询向量
     * 4. 缓存查询向量
     * 5. 返回查询向量
     */
    @Override
    public QueryEmbedding vectorize(QueryVectorizeRequest request) {
        if (request == null || request.getQueryText() == null || request.getQueryText().isBlank()) {
            throw new BizException(SearchErrorCode.SEARCH_TEXT_EMPTY);
        }

        VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();

        QueryEmbedding cachedEmbedding = textVectorCacheService.get(
                config.getModelName(),
                config.getVectorDim(),
                request.getQueryText());
        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }

        TextVectorizeRequest textRequest = TextVectorizeRequest.builder()
                .text(request.getQueryText())
                .modelName(config.getModelName())
                .traceId(request.getTraceId())
                .requestId(request.getRequestId())
                .build();

        VectorizeResult result = modelVectorizationClient.vectorizeText(textRequest);

        QueryEmbedding queryEmbedding = buildQueryEmbedding(SearchType.TEXT, result);

        textVectorCacheService.put(
                config.getModelName(),
                config.getVectorDim(),
                request.getQueryText(),
                queryEmbedding);

        return queryEmbedding;
}

    private QueryEmbedding buildQueryEmbedding(SearchType searchType, VectorizeResult result) {
        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            throw new BizException(SearchErrorCode.QUERY_VECTORIZATION_FAILED);
        }
        if (result.getEmbedding() == null || result.getEmbedding().isEmpty()) {
            throw new BizException(SearchErrorCode.QUERY_EMBEDDING_EMPTY);
        }

        return QueryEmbedding.builder()
                .searchType(searchType)
                .embedding(result.getEmbedding())
                .dim(result.getDim())
                .modelName(result.getModelName())
                .build();
    }
}
