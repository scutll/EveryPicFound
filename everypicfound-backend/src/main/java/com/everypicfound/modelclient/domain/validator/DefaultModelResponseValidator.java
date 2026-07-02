package com.everypicfound.modelclient.domain.validator;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.modelclient.domain.enums.VectorizeType;
import com.everypicfound.modelclient.error.ModelClientErrorCode;
import com.everypicfound.modelclient.infrastructure.http.PythonHealthHttpResponse;
import com.everypicfound.modelclient.infrastructure.http.PythonVectorizeHttpResponse;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultModelResponseValidator implements ModelResponseValidator {

    private static final String OPERATION_VECTORIZE = "vectorize";
    private static final String OPERATION_HEALTH = "health";

    private final MetricRecorder metricRecorder;

    @Override
    public void validateVectorizeResponse(PythonVectorizeHttpResponse response) {
        if (response == null) {
            throw invalidResponse(OPERATION_VECTORIZE, "null_response", ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        VectorizeType vectorizeType = parseVectorizeType(response.getVectorizeType());

        if (!Boolean.TRUE.equals(response.getSuccess())) {
            throw new SystemException(getVectorizeFailedCode(vectorizeType));
        }

        validateEmbedding(response.getEmbedding());
        validateDim(response.getDim(), response.getEmbedding());
        validateModelName(OPERATION_VECTORIZE, response.getModelName());

    }

    @Override
    public void validateHealthResponse(PythonHealthHttpResponse response) {
        if (response == null) {
            throw invalidResponse(
                    OPERATION_HEALTH,
                    "null_response",
                    ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        if (!Boolean.TRUE.equals(response.getSuccess()) || !Boolean.TRUE.equals(response.getModelLoaded())) {
            throw new SystemException(
                    ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE);
        }

        validateModelName(OPERATION_HEALTH, response.getModelName());

        if (response.getVectorDim() == null || response.getVectorDim() <= 0) {
            throw invalidResponse(
                    OPERATION_HEALTH,
                    "dimension_invalid",
                    ModelClientErrorCode.MODEL_DIM_MISMATCH);
        }
    }

    private VectorizeType parseVectorizeType(String vectorizeType) {
        if (vectorizeType == null || vectorizeType.isBlank()) {
            throw invalidResponse(
                    OPERATION_VECTORIZE,
                    "vectorize_type_missing",
                    ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }

        try {
            return VectorizeType.valueOf(vectorizeType.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalidResponse(
                    OPERATION_VECTORIZE,
                    "vectorize_type_invalid",
                    ModelClientErrorCode.MODEL_RESPONSE_INVALID,
                    exception);
        }
    }

    private ModelClientErrorCode getVectorizeFailedCode(VectorizeType vectorizeType) {
        return switch (vectorizeType) {
            case IMAGE -> ModelClientErrorCode.IMAGE_VECTORIZATION_FAILED;
            case TEXT -> ModelClientErrorCode.TEXT_VECTORIZATION_FAILED;
        };
    }

    private void validateEmbedding(List<Float> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            throw new BizException(ModelClientErrorCode.MODEL_EMBEDDING_EMPTY);
        }
    }

    private void validateDim(Integer dim, List<Float> embedding) {
        if (dim == null || dim <= 0 || embedding.size() != dim) {
            throw invalidResponse(
                    OPERATION_VECTORIZE,
                    "dimension_mismatch",
                    ModelClientErrorCode.MODEL_DIM_MISMATCH);
        }
    }

    private void validateModelName(String operation, String modelName) {
        if (modelName == null || modelName.isBlank()) {
            throw invalidResponse(
                    operation,
                    "model_name_missing",
                    ModelClientErrorCode.MODEL_RESPONSE_INVALID);
        }
    }


    private SystemException invalidResponse(
            String operation,
            String reason,
            ModelClientErrorCode errorCode) {
        recordInvalidResponse(operation, reason);
        return new SystemException(errorCode);
    }

    private SystemException invalidResponse(
            String operation,
            String reason,
            ModelClientErrorCode errorCode,
            Throwable cause) {

        recordInvalidResponse(operation, reason);
        return new SystemException(errorCode, cause);
    }

    private void recordInvalidResponse(
            String operation,
            String reason) {

        metricRecorder.increment(
                MetricName.MODEL_RESPONSE_INVALID,
                MetricTags.builder()
                        .tag(MetricTag.OPERATION, operation)
                        .tag(MetricTag.REASON, reason)
                        .build());
    }
}
