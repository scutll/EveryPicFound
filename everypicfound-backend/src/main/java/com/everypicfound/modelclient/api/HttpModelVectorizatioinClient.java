package com.everypicfound.modelclient.api;

import org.springframework.stereotype.Service;
import java.util.Locale;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.ModelHealthResult;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.modelclient.domain.enums.VectorizeType;
import com.everypicfound.modelclient.domain.validator.ModelResponseValidator;
import com.everypicfound.modelclient.error.ModelClientErrorCode;
import com.everypicfound.modelclient.infrastructure.config.ModelClientProperties;
import com.everypicfound.modelclient.infrastructure.http.PythonHealthHttpResponse;
import com.everypicfound.modelclient.infrastructure.http.PythonModelHttpClient;
import com.everypicfound.modelclient.infrastructure.http.PythonVectorizeHttpResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HttpModelVectorizatioinClient implements ModelVectorizationClient{
    private final PythonModelHttpClient pythonModelHttpClient;

    private final ModelResponseValidator modelResponseValidator;

    private final ModelClientProperties properties;


    @Override
    public VectorizeResult vectorizeImage(ImageVectorizeRequest request) {

        validateImageRequest(request);
        fillDefaultModelName(request);

        PythonVectorizeHttpResponse response = pythonModelHttpClient.postImageMultipart(request);
        modelResponseValidator.validateVectorizeResponse(response);

        return buildVectorizeResult(response);
    }


    @Override
    public VectorizeResult vectorizeText(TextVectorizeRequest request) {
        validateTextRequest(request);
        fillDefaultModelName(request);

        PythonVectorizeHttpResponse response = pythonModelHttpClient.postTextForm(request);
        modelResponseValidator.validateVectorizeResponse(response);

        return buildVectorizeResult(response);
    }
    
    @Override
    public ModelHealthResult checkHealth() {
        PythonHealthHttpResponse response = pythonModelHttpClient.getHealth();
        modelResponseValidator.validateHealthResponse(response);

        return ModelHealthResult.builder()
                .success(response.getSuccess())
                .status(response.getStatus())
                .modelLoaded(response.getModelLoaded())
                .modelName(response.getModelName())
                .vectorDim(response.getVectorDim())
                .device(response.getDevice())
                .message(response.getMessage())
                .build();
    }

    private VectorizeResult buildVectorizeResult(PythonVectorizeHttpResponse response) {
        return VectorizeResult.builder()
                .success(response.getSuccess())
                .vectorizeType(parseVectorizeType(response.getVectorizeType()))
                .imageId(response.getImageId())
                .embedding(response.getEmbedding())
                .dim(response.getDim())
                .modelName(response.getModelName())
                .costMs(response.getCostMs())
                .build();
    }
    
    private VectorizeType parseVectorizeType(String vectorizeType) {
        if (vectorizeType == null || vectorizeType.isBlank()) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        try {
            return VectorizeType.valueOf(vectorizeType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID, e);
        }
    }
    
    private void validateImageRequest(ImageVectorizeRequest request) {
        if (request == null) {
            throw new BizException(ModelClientErrorCode.IMAGE_INPUT_TYPE_INVALID);
        }
        if (request.getImageInputType() == null) {
            throw new BizException(ModelClientErrorCode.IMAGE_INPUT_TYPE_INVALID);
        }
    }

    private void validateTextRequest(TextVectorizeRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new BizException(ModelClientErrorCode.TEXT_VECTORIZATION_FAILED);
        }
    }

    private void fillDefaultModelName(ImageVectorizeRequest request) {
        if (request.getModelName() == null || request.getModelName().isBlank()) {
            request.setModelName(properties.getDefaultModelName());
        }
    }

    private void fillDefaultModelName(TextVectorizeRequest request) {
        if (request.getModelName() == null || request.getModelName().isBlank()) {
            request.setModelName(properties.getDefaultModelName());
        }
    } 



}
