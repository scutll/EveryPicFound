package com.everypicfound.vectorization;


import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.application.processor.ImageVectorizationProcessor;
import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;

@SpringBootTest
@ActiveProfiles("test")
public class ImageVectorizationProcessorDirectTest {
    private static final Long IMAGE_ID = 4885600175919104L;

    @Autowired
    private ImageVectorizationProcessor imageVectorizationProcessor;

    @Autowired
    private ImageAssetRepository imageAssetRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private VectorIndexClient vectorIndexClient;

    @Autowired
    private ActiveCollectionResolver activeCollectionResolver;

    @Test
    void process_shouldVectorizeExistingPendingImageAndUpsertToQdrant() {
        ImageAssetDTO before = imageAssetRepository.findById(IMAGE_ID);

        assertThat(before).isNotNull();
        assertThat(before.getImageStatus()).isEqualTo(ImageStatus.NORMAL);
        assertThat(before.getVectorStatus()).isEqualTo(VectorStatus.PENDING);
        assertThat(before.getStoragePath()).isNotBlank();
        assertThat(fileStorageService.exists(before.getStoragePath())).isTrue();

        ImageVectorizationTaskCommand command = ImageVectorizationTaskCommand.builder()
                .imageId(IMAGE_ID)
                .traceId("trace-processor-direct-test")
                .requestId("request-processor-direct-test")
                .source("PROCESSOR_DIRECT_TEST")
                .build();
        
        ImageVectorizationResult result = imageVectorizationProcessor.process(command);

        assertThat(result).isNotNull();
        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getImageId()).isEqualTo(IMAGE_ID);
        assertThat(result.getVectorStatus()).isEqualTo(VectorStatus.READY);
        assertThat(result.getFailReason()).isNull();

        ImageAssetDTO after = imageAssetRepository.findById(IMAGE_ID);

        assertThat(after).isNotNull();
        assertThat(after.getImageStatus()).isEqualTo(ImageStatus.NORMAL);
        assertThat(after.getVectorStatus()).isEqualTo(VectorStatus.READY);
        assertThat(after.getRetryCount()).isEqualTo(0);
        assertThat(after.getFailReason()).isNull();
        assertThat(after.getVectorUpdatedTime()).isNotNull();

         VectorCollectionConfig collectionConfig = activeCollectionResolver.resolveActiveCollection();

        VectorOperationResult existsResult = vectorIndexClient.exists(
                VectorExistsRequest.builder()
                        .collectionName(collectionConfig.getCollectionName())
                        .vectorId(IMAGE_ID)
                        .build()
        );

        assertThat(existsResult).isNotNull();
        assertThat(existsResult.getSuccess()).isTrue();
        assertThat(existsResult.getExists()).isTrue();
    }
}
