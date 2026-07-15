package com.everypicfound.vectorindex.collection;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;
import com.everypicfound.vectorindex.infrastructure.config.VectorIndexProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultVectorCollectionConfigProvider implements VectorCollectionConfigProvider {
    
    
    private final VectorIndexProperties properties;

    @Override
    public VectorCollectionConfig getActiveConfig() {
        String activeCollectionName = properties.getActiveCollectionName();
        VectorCollectionConfig config = getConfig(activeCollectionName);
        validateConfig(config);
        return config;
    }

    @Override
    public VectorCollectionConfig getConfig(String collectionName) {

        if (collectionName == null || collectionName.isBlank()) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        Map<String, VectorCollectionConfig> collections = properties.getCollections();
        if (collections == null || !collections.containsKey(collectionName)) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_NOT_FOUND);
        }

        return collections.get(collectionName);
    }

    @Override
    public void validateConfig(VectorCollectionConfig config){

        if(config == null){
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        if(config.getCollectionName() == null || config.getCollectionName().isBlank()){
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        if (config.getModelName() == null || config.getModelName().isBlank()) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        if (config.getVectorDim() == null || config.getVectorDim() <= 0) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        if (config.getVectorVersion() == null || config.getVectorVersion().isBlank()) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        if (config.getDistanceMetric() == null) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }
    }
    

}
