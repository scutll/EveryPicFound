package com.everypicfound.modelclient.domain.validator;


import java.util.List;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.modelclient.domain.enums.VectorizeType;
import com.everypicfound.modelclient.error.ModelClientErrorCode;
import com.everypicfound.modelclient.infrastructure.http.PythonHealthHttpResponse;
import com.everypicfound.modelclient.infrastructure.http.PythonVectorizeHttpResponse;

@Component
public class DefaultModelResponseValidator implements ModelResponseValidator {
    
    @Override
    public void validateVectorizeResponse(PythonVectorizeHttpResponse response) {
        if (response == null) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        VectorizeType vectorizeType = parseVectorizeType(response.getVectorizeType());

        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new BizException(getVectorizeFailedCode(vectorizeType));
        }

        validateEmbedding(response.getEmbedding());
        validateDim(response.getDim(), response.getEmbedding());
        validateModelName(response.getModelName());

    }
    
    @Override 
    public void validateHealthResponse(PythonHealthHttpResponse response) {
        if (response == null) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new BizException(ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE);
        }
        if (!Boolean.TRUE.equals(response.getModelLoaded())) {
            throw new BizException(ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE);
        }

        validateModelName(response.getModelName());

        if (response.getVectorDim() == null || response.getVectorDim() <= 0) {
            throw new BizException(ModelClientErrorCode.MODEL_DIM_MISMATCH);
        }
    }
    

    private VectorizeType parseVectorizeType(String vectorizeType) {
        if (vectorizeType == null || vectorizeType.isBlank()) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        try {
            return VectorizeType.valueOf(vectorizeType);
        } catch (IllegalArgumentException e) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }
    }
    
    private ModelClientErrorCode getVectorizeFailedCode(VectorizeType vectorizeType) {
        return switch (vectorizeType) {
            case IMAGE -> ModelClientErrorCode.IMAGE_VECTORIZATION_FAILED;
            case TEXT -> ModelClientErrorCode.TEXT_VECTORIZATION_FAILED;
        };
    }
    

    private void validateEmbedding(List<Float> embedding){
        if(embedding == null || embedding.isEmpty()){
            throw new BizException(ModelClientErrorCode.MODEL_EMBEDDING_EMPTY);
        }
    }


    private void validateDim(Integer dim, List<Float> embedding) {
        if (dim == null || dim <= 0 || embedding.size() != dim) {
            throw new BizException(ModelClientErrorCode.MODEL_DIM_MISMATCH);
        }
    }

    private void validateModelName(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw new BizException(ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }
    }
}
