package com.everypicfound.vectorization.domain.query;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.modelclient.domain.enums.ImageInputType;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ImageQueryVectorizer implements QueryVectorizer {
    
    private final ModelVectorizationClient modelVectorizationClient;

    private final ActiveCollectionResolver activeCollectionResolver;    

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

        ImageVectorizeRequest imageRequest = ImageVectorizeRequest.builder()
                .imageInputType(ImageInputType.MULTIPART)
                .inputStream(request.getQueryImage())
                .originalFileName(request.getQueryImageOriginalFileName())
                .fileSize(request.getQueryImageFileSize())
                .mimeType(request.getQueryImageMimeType())
                .modelName(config.getModelName())
                .traceId(request.getTraceId())
                .requestId(request.getRequestId())
                .build();

        VectorizeResult result = modelVectorizationClient.vectorizeImage(imageRequest);
        return buildQueryEmbedding(SearchType.IMAGE, result);
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
