package com.everypicfound.vectorindex.infrastructure.qdrant;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.collection.health.VectorCollectionHealthChecker;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.VectorParams;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QdrantVectorCollectionHealthChecker implements VectorCollectionHealthChecker {
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final QdrantClient qdrantClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final QdrantVectorMapper qdrantVectorMapper;
    

    @Override
    public boolean checkCollectionExists(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        try {
            return qdrantClient.collectionExistsAsync(collectionName, DEFAULT_TIMEOUT).get();
        } catch (Exception e) {
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_NOT_FOUND, e);
        }
    }
    

    @Override
    public void check() {
        try {
            qdrantClient.healthCheckAsync(DEFAULT_TIMEOUT).get();
        } catch (Exception e) {
            throw new BizException(VectorIndexErrorCode.VECTOR_INDEX_UNAVAILABLE, e);
        }
    }
    

    @Override
    public void ensureActiveCollectionReady() {
        check();

        VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();
        boolean exists = checkCollectionExists(config.getCollectionName());

        if (!exists) {
            createCollection(config);
        }
    }

    private void createCollection(VectorCollectionConfig config) {
        try{
            VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(config.getVectorDim())
                    .setDistance(qdrantVectorMapper.toQdrantDistance(config.getDistanceMetric()))
                    .build();

            
            qdrantClient.createCollectionAsync(
                    config.getCollectionName(),
                    vectorParams,
                    DEFAULT_TIMEOUT
            ).get();
        } catch(Exception e){
            throw new BizException(VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID, e);
        }
    }
}
