package com.everypicfound.modelclient.api;

import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
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
public class HttpModelVectorizationClient implements ModelVectorizationClient {

    private static final String OPERATION_IMAGE = "vectorize_image";
    private static final String OPERATION_TEXT = "vectorize_text";
    private static final String OPERATION_HEALTH = "health";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";

    private static final String SOURCE_SEARCH = "search";
    private static final String SOURCE_VECTORIZATION = "vectorization";
    private static final String SOURCE_HEALTH_CHECK = "health_check";
    private static final String SOURCE_UNKNOWN = "unknown";

    private final MetricRecorder metricRecorder;

    private final PythonModelHttpClient pythonModelHttpClient;

    private final ModelResponseValidator modelResponseValidator;

    private final ModelClientProperties properties;

    @Override
    public VectorizeResult vectorizeImage(ImageVectorizeRequest request) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;
        String source = resolveSource();

        try{

            validateImageRequest(request);
            fillDefaultModelName(request);
    
            PythonVectorizeHttpResponse response = pythonModelHttpClient.postImageMultipart(request);
            modelResponseValidator.validateVectorizeResponse(response);
    
            VectorizeResult vectorizeResult = buildVectorizeResult(response);
            result = RESULT_SUCCESS;
            return vectorizeResult;
        } catch (BizException exception) {
            result = RESULT_FAILED;
            throw exception;
        } finally {
            recordClientMetrics(OPERATION_IMAGE, source, result, startNanos);
        }
    }

    @Override
    public VectorizeResult vectorizeText(TextVectorizeRequest request) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;
        String source = resolveSource();

        try {
            validateTextRequest(request);
            fillDefaultModelName(request);

            PythonVectorizeHttpResponse response = pythonModelHttpClient.postTextForm(request);

            modelResponseValidator.validateVectorizeResponse(response);

            VectorizeResult vectorizeResult = buildVectorizeResult(response);

            result = RESULT_SUCCESS;
            return vectorizeResult;
        } catch (BizException exception) {
            result = RESULT_REJECTED;
            throw exception;
        } finally {
            recordClientMetrics(
                    OPERATION_TEXT,
                    source,
                    result,
                    startNanos);
        }
    }

    @Override
    public ModelHealthResult checkHealth() {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            PythonHealthHttpResponse response = pythonModelHttpClient.getHealth();

            modelResponseValidator.validateHealthResponse(response);

            ModelHealthResult healthResult = ModelHealthResult.builder()
                    .success(response.getSuccess())
                    .status(response.getStatus())
                    .modelLoaded(response.getModelLoaded())
                    .modelName(response.getModelName())
                    .vectorDim(response.getVectorDim())
                    .device(response.getDevice())
                    .message(response.getMessage())
                    .build();

            result = RESULT_SUCCESS;
            return healthResult;
        } finally {
            recordClientMetrics(
                    OPERATION_HEALTH,
                    SOURCE_HEALTH_CHECK,
                    result,
                    startNanos);
        }
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

    private void recordClientMetrics(
        String operation,
        String source,
        String result,
        long startNanos) {

    MetricTags tags = MetricTags.builder()
            .tag(MetricTag.OPERATION, operation)
            .tag(MetricTag.SOURCE, source)
            .tag(MetricTag.RESULT, result)
            .build();

    metricRecorder.increment(
            MetricName.MODEL_CLIENT_REQUESTS,
            tags);

    metricRecorder.recordTimer(
            MetricName.MODEL_CLIENT_DURATION,
            elapsedMillis(startNanos),
            tags);
}


    private String resolveSource() {
        RequestContext context = RequestContextHolder.get();

        if (context == null
                || context.getModule() == null
                || context.getModule().isBlank()) {
            return SOURCE_UNKNOWN;
        }

        String module = context.getModule()
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (module) {
            case SOURCE_SEARCH -> SOURCE_SEARCH;
            case SOURCE_VECTORIZATION -> SOURCE_VECTORIZATION;
            default -> SOURCE_UNKNOWN;
        };
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }
    

}
