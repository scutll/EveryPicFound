package com.everypicfound.search.domain.collection;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultSearchCollectionResolver implements SearchCollectionResolver{
    

    private final ActiveCollectionResolver activeCollectionResolver;

    @Override
    public SearchCollectionContext resolve() {
        VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();
        validate(config);

        return SearchCollectionContext.builder()
                .collectionName(config.getCollectionName())
                .modelName(config.getModelName())
                .vectorDim(config.getVectorDim())
                .vectorVersion(config.getVectorVersion())
                .distanceMetric(config.getDistanceMetric())
                .build();
    }

    private void validate(VectorCollectionConfig config) {
            if (config == null
                || isBlank(config.getCollectionName())
                || isBlank(config.getModelName())
                || config.getVectorDim() == null
                || config.getVectorDim() <= 0
                || isBlank(config.getVectorVersion())
                || config.getDistanceMetric() == null) {
            throw new BizException(SearchErrorCode.SEARCH_COLLECTION_UNAVAILABLE);
        }
    }
    
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
