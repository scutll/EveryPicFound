package com.everypicfound.vectorindex;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.collection.health.VectorCollectionHealthChecker;
import com.everypicfound.vectorindex.domain.VectorDeleteRequest;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorindex.domain.VectorPayload;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


@SpringBootTest
public class VectorIndexIntegrationTest {
    
    @Autowired
    private VectorCollectionHealthChecker healthChecker;

    @Autowired
    private ActiveCollectionResolver activeCollectionResolver;

    @Autowired
    private VectorIndexClient vectorIndexClient;


    // @Test
    void shouldUpsertExistsAndDeleteVector() {
        healthChecker.ensureActiveCollectionReady();

        VectorCollectionConfig collectionConfig = activeCollectionResolver.resolveActiveCollection();

        Long vectorId = 100001L;

        VectorUpsertRequest upsertRequest = VectorUpsertRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(vectorId)
                .embedding(mockEmbedding(collectionConfig.getVectorDim()))
                .payload(VectorPayload.builder()
                        .createdTime(LocalDateTime.now())
                        .build())

                .build();
        

        VectorOperationResult upsertResult = vectorIndexClient.upsert(upsertRequest);
        assertTrue(Boolean.TRUE.equals(upsertResult.getSuccess()));

        VectorOperationResult existsResult = vectorIndexClient.exists(VectorExistsRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(vectorId)
                .build());

        assertTrue(Boolean.TRUE.equals(existsResult.getSuccess()));
        assertTrue(Boolean.TRUE.equals(existsResult.getExists()));

        VectorOperationResult deleteResult = vectorIndexClient.delete(VectorDeleteRequest.builder()
        .collectionName(collectionConfig.getCollectionName())
        .vectorId(vectorId)
        .build());

        assertTrue(Boolean.TRUE.equals(deleteResult.getSuccess()));

        VectorOperationResult existsAfterDeleteResult = vectorIndexClient.exists(VectorExistsRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(vectorId)
                .build());

        assertTrue(Boolean.TRUE.equals(existsAfterDeleteResult.getSuccess()));
        assertFalse(Boolean.TRUE.equals(existsAfterDeleteResult.getExists()));

    }

    @Test
    void justUpsertAndExistsVector(){
        healthChecker.ensureActiveCollectionReady();

        VectorCollectionConfig collectionConfig = activeCollectionResolver.resolveActiveCollection();

        Long vectorId = 100001L;

        VectorUpsertRequest upsertRequest = VectorUpsertRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(vectorId)
                .embedding(mockEmbedding(collectionConfig.getVectorDim()))
                .payload(VectorPayload.builder()
                        .createdTime(LocalDateTime.now())
                        .build())

                .build();
        

        VectorOperationResult upsertResult = vectorIndexClient.upsert(upsertRequest);
        assertTrue(Boolean.TRUE.equals(upsertResult.getSuccess()));

        VectorOperationResult existsResult = vectorIndexClient.exists(VectorExistsRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(vectorId)
                .build());

        assertTrue(Boolean.TRUE.equals(existsResult.getSuccess()));
        assertTrue(Boolean.TRUE.equals(existsResult.getExists()));
    }
    
    

    private List<Float> mockEmbedding(Integer dim) {
        List<Float> embedding = new ArrayList<>(dim);
        for (int i = 0; i < dim; i++) {
            embedding.add((float) i / dim);
        }

        return embedding;
    }
}
