package com.everypicfound.imageasset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ImageUploadOnlyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    //用于断言数据库结果
    @Autowired
    private ImageAssetRepository imageAssetRepository;

    @MockitoBean
    private VectorizationTaskPublisher vectorizationTaskPublisher;


    @Test
    void upload_shouldSaveImageAssetAndPublishTask() throws Exception {
        Mockito.when(vectorizationTaskPublisher.publish(any(ImageVectorizationTaskCommand.class)))
                    .thenReturn(VectorizationPublishResult.builder()
                                .success(true)
                                .message("mock submitted")
                                .build());
                                
        MockMultipartFile file = new MockMultipartFile(
                "imageFile", "test.jpg", "image/jpeg", Files.readAllBytes(Path.of("src/test/resources/test-images/test.jpg")));
        
        MvcResult mvcResult = mockMvc.perform(multipart("/api/images/upload").file(file))
                .andExpect(status().isOk())
                .andReturn();
        
        String responseBody = mvcResult.getResponse().getContentAsString();
        JsonNode root = objectMapper.readTree(responseBody);

        assertThat(root.get("code").asInt()).isEqualTo(0);
        assertThat(root.get("message").asText()).isEqualTo("success");

        JsonNode data = root.get("data");
        assertThat(data).isNotNull();

        Long imageId = data.get("imageId").asLong();
        assertThat(imageId).isNotNull();
        assertThat(imageId).isPositive();


        assertThat(data.get("originalFileName").asText()).isEqualTo("test.jpg");
        assertThat(data.get("imageUrl").asText()).isNotBlank();

        ImageAssetDTO imageAsset = imageAssetRepository.findById(imageId);
        assertThat(imageAsset).isNotNull();

        assertThat(imageAsset.getId()).isEqualTo(imageId);
        assertThat(imageAsset.getOriginalFileName()).isEqualTo("test.jpg");
        assertThat(imageAsset.getFileName()).isNotBlank();
        assertThat(imageAsset.getFileHash()).isNotBlank();
        assertThat(imageAsset.getFileSize()).isGreaterThan(0L);
        assertThat(imageAsset.getMimeType()).isEqualTo("image/jpeg");
        assertThat(imageAsset.getFileExt()).isEqualTo("jpg");
        assertThat(imageAsset.getStoragePath()).isNotBlank();

        assertThat(imageAsset.getImageStatus()).isEqualTo(ImageStatus.NORMAL);
        assertThat(imageAsset.getVectorStatus()).isEqualTo(VectorStatus.PENDING);
        assertThat(imageAsset.getRetryCount()).isEqualTo(0);

        ArgumentCaptor<ImageVectorizationTaskCommand> captor = ArgumentCaptor
                .forClass(ImageVectorizationTaskCommand.class);

        verify(vectorizationTaskPublisher, times(1)).publish(captor.capture());

        ImageVectorizationTaskCommand taskCommand = captor.getValue();
        assertThat(taskCommand.getImageId()).isEqualTo(imageId);
        assertThat(taskCommand.getSource()).isEqualTo("IMAGE_UPLOAD");
        assertThat(taskCommand.getTraceId()).isNotBlank();
        assertThat(taskCommand.getRequestId()).isNotBlank();

        Mockito.verifyNoMoreInteractions(vectorizationTaskPublisher);
    }



}
