package com.everypicfound.modelclient.infrastructure.interfaces;

import java.io.IOException;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.response.Result;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.ModelHealthResult;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.modelclient.domain.enums.ImageInputType;
import com.everypicfound.modelclient.error.ModelClientErrorCode;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;



@Profile("dev")
@RestController
public class ModelClientTestController {
    
    private final ModelVectorizationClient modelVectorizationClient;

    public ModelClientTestController(ModelVectorizationClient modelVectorizationClient) {
        this.modelVectorizationClient = modelVectorizationClient;
    }


    @GetMapping("/dev/modelclient/health")
    public Result<ModelHealthResult> health() {
        return Result.success(modelVectorizationClient.checkHealth(), "test_modelclient_health");
    }

    @PostMapping("/dev/modelclient/vectorize/text")
    public Result<VectorizeResult> vectorizeText(@RequestParam String text) {
        TextVectorizeRequest request = TextVectorizeRequest.builder()
                .text(text)
                .traceId("test-modelclient-text")
                .requestId("test-modelclient-text-request")
                .build();
        return Result.success(modelVectorizationClient.vectorizeText(request), "test_modelclient_vectorize_text");
    }

    @PostMapping("/dev/modelclient/vectorize/image")
    public Result<VectorizeResult> vectorizeImage(@RequestParam("file") MultipartFile file) {
        try{
            ImageVectorizeRequest request = ImageVectorizeRequest.builder()
                    .imageInputType(ImageInputType.MULTIPART)
                    .inputStream(file.getInputStream())
                    .originalFileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .traceId("test-modelclient-image")
                    .requestId("test-modelclient-image-request")
                    .build();
    
            return Result.success(modelVectorizationClient.vectorizeImage(request), "test_modelclient_vectorize_image");
        } catch (IOException e) {
            throw new BizException(ModelClientErrorCode.IMAGE_VECTORIZATION_FAILED, e);
        }
    }
    
    
}
