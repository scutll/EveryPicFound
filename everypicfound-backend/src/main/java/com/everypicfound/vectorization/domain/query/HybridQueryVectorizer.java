package com.everypicfound.vectorization.domain.query;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorization.config.HybridFusionProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class HybridQueryVectorizer implements QueryVectorizer{
    
    private final ImageQueryVectorizer imageQueryVectorizer;

    private final TextQueryVectorizer textQueryVectorizer;

    private final HybridFusionStrategy hybridFusionStrategy;

    private final HybridFusionProperties hybridFusionProperties;


    @Override
    public SearchType supportType() {
        return SearchType.HYBRID;
    }

    @Override
    public QueryEmbedding vectorize(QueryVectorizeRequest request) {
        if (request == null) {
            throw new BizException(SearchErrorCode.SEARCH_PARAM_INVALID);
        }
        QueryEmbedding imageEmbedding = imageQueryVectorizer.vectorize(request);
        QueryEmbedding textEmbedding = textQueryVectorizer.vectorize(request);

        if (imageEmbedding.getDim() == null
                || textEmbedding.getDim() == null
                || !imageEmbedding.getDim().equals(textEmbedding.getDim())) {
            throw new BizException(SearchErrorCode.QUERY_VECTOR_DIM_MISMATCH);
        }

        HybridFusionResult fusionResult = hybridFusionStrategy.fuse(
            HybridFusionRequest.builder()
                        .imageEmbedding(imageEmbedding.getEmbedding())
                        .textEmbedding(textEmbedding.getEmbedding())
                        .imageWeight(hybridFusionProperties.getImageWeight())
                        .textWeight(hybridFusionProperties.getTextWeight())
                        .normalizeEnabled(hybridFusionProperties.getNormalizeEnabled())
                        .build()
        );

        return QueryEmbedding.builder()
                .searchType(SearchType.HYBRID)
                .embedding(fusionResult.getHybridEmbedding())
                .dim(fusionResult.getDim())
                .modelName(imageEmbedding.getModelName())
                .build();

    }
}
