package com.everypicfound.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;



@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UploadVectorizationEndToEndTest {
    
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageAssetRepository imageAssetRepository;

    @Autowired
    private VectorIndexClient vectorIndexClient;

    @Autowired
    private ActiveCollectionResolver activeCollectionResolver;

    @Test
    void upload_shouldTriggerVectorizationAndUpsertVector() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "imageFile",
                "side1.jpg",
                "image/jpeg",
                Files.readAllBytes(Path.of("src/test/resources/test-images/side1.jpg")));

        MvcResult mvcResult = mockMvc.perform(multipart("/api/images/upload").file(file))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString());

        assertThat(root.get("code").asInt()).isEqualTo(0);
        assertThat(root.get("message").asText()).isEqualTo("success");

        JsonNode data = root.get("data");
        assertThat(data).isNotNull();

        Long imageId = data.get("imageId").asLong();
        assertThat(imageId).isPositive();

        ImageAssetDTO initialAsset = imageAssetRepository.findById(imageId);
        assertThat(initialAsset).isNotNull();
        assertThat(initialAsset.getImageStatus()).isEqualTo(ImageStatus.NORMAL);
        assertThat(initialAsset.getVectorStatus()).isIn(VectorStatus.PENDING, VectorStatus.PROCESSING,
                VectorStatus.READY);

        ImageAssetDTO readyAsset = waitUntilReady(imageId, Duration.ofSeconds(60));

        assertThat(readyAsset.getImageStatus()).isEqualTo(ImageStatus.NORMAL);
        assertThat(readyAsset.getVectorStatus()).isEqualTo(VectorStatus.READY);
        assertThat(readyAsset.getRetryCount()).isEqualTo(0);
        assertThat(readyAsset.getFailReason()).isNull();
        assertThat(readyAsset.getProcessingStartedTime()).isNotNull();
        assertThat(readyAsset.getVectorUpdatedTime()).isNotNull();
        assertThat(readyAsset.getStoragePath()).isNotBlank();

        VectorCollectionConfig collectionConfig = activeCollectionResolver.resolveActiveCollection();

        VectorOperationResult existsResult = vectorIndexClient.exists(VectorExistsRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(imageId)
                .build());

        assertThat(existsResult).isNotNull();
        assertThat(existsResult.getSuccess()).isTrue();
        assertThat(existsResult.getExists()).isTrue();
    }
    
    private ImageAssetDTO waitUntilReady(Long imageId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();

        ImageAssetDTO latest = null;

        while (System.currentTimeMillis() < deadline) {
            latest = imageAssetRepository.findById(imageId);

            if (latest != null && VectorStatus.READY.equals(latest.getVectorStatus())) {
                return latest;
            }

            if (latest != null && VectorStatus.FAILED.equals(latest.getVectorStatus())) {
                throw new AssertionError("vectorization failed, imageId="
                        + imageId
                        + ", failReason="
                        + latest.getFailReason());
            }

            Thread.sleep(1000);
        }

        throw new AssertionError("wait vectorization ready timeout, imageId="
                + imageId
                + ", latestStatus="
                + (latest == null ? null : latest.getVectorStatus())
                + ", failReason="
                + (latest == null ? null : latest.getFailReason()));
    }


}
