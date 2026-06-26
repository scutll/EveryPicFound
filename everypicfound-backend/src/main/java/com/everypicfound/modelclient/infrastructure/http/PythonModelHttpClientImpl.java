package com.everypicfound.modelclient.infrastructure.http;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.client.
SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.error.ModelClientErrorCode;
import com.everypicfound.modelclient.infrastructure.config.ModelClientProperties;

@Component
public class PythonModelHttpClientImpl implements PythonModelHttpClient {

    private static final String ENDPOINT_IMAGE = "vectorize_image";
    private static final String ENDPOINT_TEXT = "vectorize_text";
    private static final String ENDPOINT_HEALTH = "health";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_TIMEOUT = "timeout";
    private static final String RESULT_UNAVAILABLE = "unavailable";
    private static final String RESULT_FAILED = "failed";

    private final MetricRecorder metricRecorder;
    
    private final RestClient restClient;

    private final ModelClientProperties properties;

    public PythonModelHttpClientImpl(RestClient.Builder restClientBuilder,
                                    ModelClientProperties properties,
                                    MetricRecorder metricRecorder) {
        this.metricRecorder = metricRecorder;
        this.properties = properties;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();

    }

    @Override
    public PythonVectorizeHttpResponse postImageMultipart(ImageVectorizeRequest request) {
        /*
         * 参数校验放在计时之前。
         * 参数非法时根本没有发起 HTTP 请求，不应污染 HTTP 请求指标。
         */
        validateImageRequest(request);

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try{
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("imageId", request.getImageId());
            body.add("traceId", request.getTraceId());
            body.add("requestId", request.getRequestId());
            body.add("file", buildFilePart(request));


            PythonVectorizeHttpResponse response = restClient.post()
                .uri(buildUrl(properties.getImageVectorizePath()))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                    .body(PythonVectorizeHttpResponse.class);
            result = RESULT_SUCCESS;
            return response;
        } catch (ResourceAccessException exception) {
            ModelClientErrorCode errorCode = resolveAccessError(exception);
            result = resolveAccessResult(errorCode);

            throw new SystemException(errorCode, exception);


        } catch (RestClientException exception) {
            result = RESULT_FAILED;
            throw new SystemException(ModelClientErrorCode.MODEL_SERVICE_ERROR, exception);
        } finally {
            recordHttpMetrics(
                ENDPOINT_IMAGE,
                result,
                startNanos
            );
        }
        
    }


    @Override
    public PythonVectorizeHttpResponse postTextForm(TextVectorizeRequest request) {
        validateTextRequest(request);

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try{
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("traceId", request.getTraceId());
            body.add("requestId", request.getRequestId());
            body.add("text", request.getText());

            PythonVectorizeHttpResponse response = restClient.post()
                    .uri(buildUrl(properties.getTextVectorizePath()))
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(PythonVectorizeHttpResponse.class);
            result = RESULT_SUCCESS;
            return response;
        } catch (ResourceAccessException exception) {
            ModelClientErrorCode errorCode = resolveAccessError(exception);
            result = resolveAccessResult(errorCode);

            throw new SystemException(errorCode, exception);
        } catch (RestClientException exception) {
            result = RESULT_FAILED;
            throw new SystemException(ModelClientErrorCode.MODEL_SERVICE_ERROR, exception);
        } finally {
            recordHttpMetrics(ENDPOINT_TEXT, result, startNanos);
        }
        
    }


    @Override
    public PythonHealthHttpResponse getHealth() {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            PythonHealthHttpResponse response = restClient.get()
                    .uri(buildUrl(properties.getHealthPath()))
                    .retrieve()
                    .body(PythonHealthHttpResponse.class);
            result = RESULT_SUCCESS;
            return response;
        } catch (ResourceAccessException exception) {
            ModelClientErrorCode errorCode = resolveAccessError(exception);
            result = resolveAccessResult(errorCode);

            throw new SystemException(
                    errorCode,
                    exception);
        } catch (RestClientException exception) {
            result = RESULT_FAILED;

            throw new SystemException(
                    ModelClientErrorCode.MODEL_SERVICE_ERROR,
                    exception);
        } finally {
            recordHttpMetrics(
                    ENDPOINT_HEALTH,
                    result,
                    startNanos);
        }
    }
    
    private HttpEntity<InputStreamResource> buildFilePart(ImageVectorizeRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(resolveMediaType(request.getMimeType()));

        InputStreamResource resource = new InputStreamResource(request.getInputStream()){
            @Override
            public String getFilename(){
                return request.getOriginalFileName();
            }

            @Override
            public long contentLength(){
                return request.getFileSize() == null? -1 : request.getFileSize();
            }
        };

        return new HttpEntity<>(resource, headers);

    }
    
    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        return MediaType.parseMediaType(mimeType);
    }

    private void validateImageRequest(ImageVectorizeRequest request) {
        if (request == null || request.getInputStream() == null) {
            throw new BizException(ModelClientErrorCode.IMAGE_INPUT_TYPE_INVALID);
        }
    }

    private void validateTextRequest(TextVectorizeRequest request) {
        if (request == null || request.getText() == null || request.getText().isBlank()) {
            throw new BizException(ModelClientErrorCode.TEXT_INPUT_INVALID);
        }
    }


    private String buildUrl(String path) {
        String baseUrl = properties.getBaseUrl();
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + path;
        }
        if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        }

        return baseUrl + path;
    }

    private ModelClientErrorCode resolveAccessError(ResourceAccessException e) {
        return isTimeout(e) ? ModelClientErrorCode.MODEL_SERVICE_TIMEOUT
                : ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE;
    }
    
    private boolean isTimeout(Throwable throwable) {
        Throwable current = throwable;

        //这里的循环是把current一层层拆开，只有有一层是Timeout就是超时引发的问题
        while (current != null) {
            if (current instanceof SocketTimeoutException) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }

    private void recordHttpMetrics(String endpoint, String result, long startNanos) {
        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.ENDPOINT, endpoint)
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.MODEL_HTTP_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.MODEL_HTTP_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private String resolveAccessResult(ModelClientErrorCode errorCode) {
        return errorCode == ModelClientErrorCode.MODEL_SERVICE_TIMEOUT
                ?RESULT_TIMEOUT : RESULT_UNAVAILABLE;
    }
    
    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
    }

}
