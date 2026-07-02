package com.everypicfound.vectorization.domain.query;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.springframework.stereotype.Component;

import com.everypicfound.common.cache.CacheKeyBuilder;
import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.modelclient.domain.enums.ImageInputType;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorization.domain.cache.QueryVectorCacheService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImageQueryVectorizer implements QueryVectorizer {
    
    private final ModelVectorizationClient modelVectorizationClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final QueryVectorCacheService queryVectorCacheService;
    
    private final CacheKeyBuilder cacheKeyBuilder;

    @Override
    public SearchType supportType() {
        return SearchType.IMAGE;
    }

    @Override
    public QueryEmbedding vectorize(QueryVectorizeRequest request) {
        if (request == null || request.getQueryImage() == null) {
            throw new BizException(SearchErrorCode.SEARCH_IMAGE_EMPTY);
        }

        VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();

        byte[] imageBytes;
        try{
            imageBytes = request.getQueryImage().readAllBytes();
        } catch (IOException exception) {
            throw new SystemException(SearchErrorCode.QUERY_VECTORIZATION_FAILED, exception);
        }

        String rawKey = String.join(
    ":",    
                SearchType.IMAGE.name(),
                config.getModelName(),
                config.getVectorVersion(),
                String.valueOf(config.getVectorDim()),
                cacheKeyBuilder.hashImage(imageBytes));

        String key = cacheKeyBuilder.buildQueryVectorKey(rawKey);

        QueryEmbedding cachedEmbedding = queryVectorCacheService.get(key, config.getVectorDim());

        if (cachedEmbedding != null) {
            return cachedEmbedding;
        }


        ImageVectorizeRequest imageRequest = ImageVectorizeRequest.builder()
                .imageInputType(ImageInputType.MULTIPART)
                .inputStream(new ByteArrayInputStream(imageBytes))
                .originalFileName(request.getQueryImageOriginalFileName())
                .fileSize(request.getQueryImageFileSize())
                .mimeType(request.getQueryImageMimeType())
                .modelName(config.getModelName())
                .traceId(request.getTraceId())
                .requestId(request.getRequestId())
                .build();

        VectorizeResult result = modelVectorizationClient.vectorizeImage(imageRequest);

        QueryEmbedding queryEmbedding = buildQueryEmbedding(SearchType.IMAGE, result);

        queryVectorCacheService.put(key, config.getVectorDim(), queryEmbedding);

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

}
