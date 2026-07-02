package com.everypicfound.imageasset.interfaces.controller;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.web.bind.annotation.RestController;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.error.StorageErrorCode;
import com.everypicfound.storage.infrastructure.config.StorageProperties;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.GetMapping;

/**
 * 图片文件访问接口。
 *
 * <p>
 * 图片访问分为两个阶段：
 * </p>
 *
 * <ol>
 * <li>同步准备阶段：解析路径、读取 StorageResource、构造响应；</li>
 * <li>流式输出阶段：将 InputStream 数据写入客户端 OutputStream。</li>
 * </ol>
 *
 * <p>
 * 同步准备阶段异常继续向上抛出，由 GlobalExceptionHandler
 * 统一记录；流式输出阶段可能在响应提交后执行，因此由本类
 * 作为最终责任边界记录流式异常。
 * </p>
 * 
 * <p>
 * StreamingResponseBody使用Spring MVC异步请求机制，Controller返回一个如何写数据的回调，Spring释放原Servlet容器线程，如何让异步TaskExecutor中的线程执行writeTo(OutputStream)
 * 应用可以直接写响应流而不持续占用 Servlet 容器线程
 * 这个机制用于隔离servlet用于处理请求的线程和TaskExecutor实际执行发送任务的线程，让大文件的发送不会长期占用servlet线程导致无空余线程来处理http请求
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ImageFileController {

    private static final String MODULE = "image-asset";

    private static final String BIZ_TYPE = "IMAGE_ACCESS";

    private static final String OPERATION_STREAM = "stream-image";

    private static final String RESULT_SUCCESS = "success";

    private static final String RESULT_FAILED = "failed";

    private static final String RESULT_NOT_FOUND = "not_found";

    private static final String RESULT_INVALID_PATH = "invalid_path";

    private static final String RESULT_CLIENT_ABORTED = "client_aborted";

    /**
     * 单次流式读取缓冲区大小。
     */
    private static final int STREAM_BUFFER_SIZE = 16 * 1024;

    /**
     * 进入日志的路径最大长度。
     */
    private static final int MAX_LOG_PATH_LENGTH = 256;

    private final FileStorageService fileStorageService;

    private final StorageProperties storageProperties;

    private final MetricRecorder metricRecorder;

    private final LogService logService;

    /**
     * 获取图片文件。
     *
     * <p>
     * 图片访问总耗时并不是在 Controller 返回 ResponseEntity
     * 时结束，而是在 StreamingResponseBody 完成写出时结束。
     * </p>
     */
    @GetMapping("/images/**")
    public ResponseEntity<StreamingResponseBody> getImage(HttpServletRequest request) {

        long requestStartNanos = System.nanoTime();
        RequestContext requestContext = RequestContextHolder.get();

        /*
         * StreamingResponseBody 可能在另一个线程执行，
         * 因此不能在流式 lambda 中再次依赖 ThreadLocal。
         */
        String requestId = requestContext == null
                ? null
                : requestContext.getRequestId();

        String traceId = requestContext == null
                ? null
                : requestContext.getTraceId();

        StorageResource resource = null;

        try {
            String storagePath = resolveStoragePath(request);

            resource = readStorageResource(storagePath);

            validateStorageResource(resource);

            recordResponseSize(resource.getFileSize());

            // 这里是相当于new 了一个对象，重写了writeTo方法
            StreamingResponseBody responseBody = buildStreamingResponseBody(resource, storagePath, requestId, traceId,
                    requestStartNanos);

            ResponseEntity.BodyBuilder responseBodyBuilder = ResponseEntity.ok()
                    .contentType(resolveMediaType(resource.getMimeType()))
                    .cacheControl(CacheControl.noCache());

            /*
             * contentLength 只能设置非负值。
             * 如果无法获取文件大小，则让容器使用分块传输。
             */
            if (resource.getFileSize() != null && resource.getFileSize() >= 0L) {
                responseBodyBuilder.contentLength(resource.getFileSize());
            }

            return responseBodyBuilder.body(responseBody);
        } catch (RuntimeException exception) {
            /*
             * 此时还处于同步 Controller 链路，
             * 异常继续交给 GlobalExceptionHandler。
             *
             * 这里只记录指标，不重复记录错误日志。
             */

            closeResourceAfterPreparationFailure(resource, exception);

            recordAccessCompletion(resolvePreparationResult(exception), requestStartNanos);

            throw exception;
        }
    }

    private StorageResource readStorageResource(String storagePath) {
        try{
            return fileStorageService.read(storagePath);
        } catch (SystemException exception) {
            if (StorageErrorCode.FILE_NOT_FOUND.equals(exception.getErrorCode())) {

                metricRecorder.increment(
                        MetricName.STORAGE_MISSING_FILES,
                        MetricTags.builder()
                                .tag(
                                        MetricTag.SOURCE,
                                        "image_access")
                                .build());
            }
            
            throw exception;
        }
    }
    
    /**
     * 构造流式响应体。
     *
     * <p>
     * StreamingResponseBody 执行时，Controller 方法通常已经返回，
     * HTTP 响应也可能已经提交。因此流式异常未必能够再转换为标准
     * Result 响应，必须在这里完成最终观测。
     * 这里返回的是一段lambda方法体，还没有真正执行，而是等待spring后面调用才开始发送
     * 相当于重写了StreamResponseBody的writeTo方法
     * </p>
     * 
     * Compiler看到返回类型明确是StreamingResponseBody并且该接口只有一个抽象方法writeTo(函数式接口 Functional Interface), 便可推断出return ... 要转换为writeTo重写，并且outputStream 对应的参数类型就是OutputStream
     */
    private StreamingResponseBody buildStreamingResponseBody(StorageResource resource,
            String storagePath,
            String requestId,
            String traceId,
            long requestStartNanos) {
        return outputStream -> {
            long streamStartNanos = System.nanoTime();

            long transferredBytes = 0;

            String streamResult = RESULT_FAILED;

            try(InputStream inputStream = resource.getInputStream()){
                byte[] buffer = new byte[STREAM_BUFFER_SIZE];

                int readLength;

                while((readLength = inputStream.read(buffer)) != -1){
                    /*
                     * 只有 write 正常完成后，才算实际完成本批字节传输。
                     */
                    outputStream.write(buffer, 0, readLength);

                    transferredBytes += readLength;
                }
                outputStream.flush();
    
                streamResult = RESULT_SUCCESS;
            }catch(IOException exception){
            streamResult = isClientAbort(exception)
                        ? RESULT_CLIENT_ABORTED
                        : RESULT_FAILED;

                /*
                客户端自行退出导致的发送中断不记入Error堆栈中
                */
                if (!RESULT_CLIENT_ABORTED.equals(
                        streamResult)) {

                    recordStreamFailureSafely(
                            requestId,
                            traceId,
                            storagePath,
                            transferredBytes,
                            exception);
                }

                /*
                 * 仍然向 Servlet 容器抛出，让容器正确结束异步请求。
                 */
                throw exception;
            }catch (RuntimeException exception) {
                streamResult = isClientAbort(exception)
                        ? RESULT_CLIENT_ABORTED
                        : RESULT_FAILED;

                if (!RESULT_CLIENT_ABORTED.equals(
                        streamResult)) {

                    recordStreamFailureSafely(
                            requestId,
                            traceId,
                            storagePath,
                            transferredBytes,
                            exception);
                }

                throw exception;
            } finally{
                  /*
                 * 无论成功、客户端中断还是服务器端失败，
                 * 都记录流式阶段和完整访问结果。
                 */
                recordStreamMetrics(
                        streamResult,
                        streamStartNanos,
                        transferredBytes);

                recordAccessCompletion(
                        streamResult,
                        requestStartNanos);
            }
        };
    }



    /**
     * 验证 Storage 返回的资源。
     */
    private void validateStorageResource(
            StorageResource resource) {

        if (resource == null
                || resource.getInputStream() == null) {

            throw new SystemException(
                    StorageErrorCode.FILE_READ_FAILED);
        }
    }

    /**
     * 从 HTTP URI 中解析相对存储路径。
     */
    private String resolveStoragePath(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }


        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        String accessUrlPrefix = normalizeAccessUrlPrefix(storageProperties.getAccessUrlPrefix());

        if (!requestUri.startsWith(accessUrlPrefix + "/")) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }

        String encodedStoragePath = requestUri.substring((accessUrlPrefix + "/").length());
        final String storagePath;
        
        try{
            storagePath = URLDecoder.decode(encodedStoragePath, StandardCharsets.UTF_8);

        } catch(IllegalArgumentException exception){
            /*
             * 非法百分号编码等问题统一转换为路径非法异常，
             * 同时保留原始 cause。
             */
            throw new SystemException(
                    StorageErrorCode.STORAGE_PATH_INVALID,
                    exception);
        }
        

        if (storagePath.isBlank() || storagePath.contains("..")) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }

        return storagePath;
    }


    /**
     * 标准化访问路径前缀。
     */
    private String normalizeAccessUrlPrefix(String accessUrlPrefix) {
        
        /*
         * Controller 映射为 /images/**，
         * 所以默认值也必须是 /images。
         */
        if (accessUrlPrefix == null || accessUrlPrefix.isBlank()) {
            return "/image";
        }

        String result = accessUrlPrefix.trim();

        if (!result.startsWith("/")) {
            result = "/" + result;
        }

        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    /**
     * 解析图片 MIME 类型。
     *
     * <p>
     * Storage 返回的 MIME 类型即使异常，也不应影响文件本身访问，
     * 因此非法值回退到 application/octet-stream。
     * </p>
     */
    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 同步准备失败时关闭已经打开的 InputStream。
     *
     * <p>
     * 关闭失败不单独记录一条错误日志，而是作为 suppressed
     * exception 附加到原始异常上，最终由 GlobalExceptionHandler
     * 一次性记录。
     * </p>
     */
    private void closeResourceAfterPreparationFailure(
            StorageResource resource,
            RuntimeException originalException) {
        if (resource == null || resource.getInputStream() == null) {
            return;
        }

        try {
            resource.getInputStream().close();
        } catch (IOException closeException) {
            originalException.addSuppressed(closeException);
        }
    }
    
    /**
     * 根据同步准备异常生成有限结果标签。
     */
    private String resolvePreparationResult(
            RuntimeException exception) {

        if (!(exception instanceof SystemException systemException)) {

            return RESULT_FAILED;
        }

        if (StorageErrorCode.STORAGE_PATH_INVALID
                .equals(systemException.getErrorCode())) {

            return RESULT_INVALID_PATH;
        }

        if (StorageErrorCode.FILE_NOT_FOUND
                .equals(systemException.getErrorCode())) {

            return RESULT_NOT_FOUND;
        }

        return RESULT_FAILED;
    }


    /**
     * 判断异常是否由客户端主动断开连接引起。
     *
     * <p>
     * 不直接依赖 Tomcat 的 ClientAbortException，
     * 避免 Controller 和具体 Servlet 容器实现耦合。
     * </p>
     */
    private boolean isClientAbort(
            Throwable throwable) {

        Throwable current = throwable;

        int depth = 0;

        while (current != null && depth < 10) {
            String className = current.getClass()
                    .getSimpleName()
                    .toLowerCase(Locale.ROOT);

            if (className.contains(
                    "clientabortexception")
                    || className.contains(
                            "asyncrequestnotusableexception")) {

                return true;
            }

            String message = current.getMessage();

            if (message != null) {
                String normalizedMessage = message.toLowerCase(
                        Locale.ROOT);

                if (normalizedMessage.contains(
                        "broken pipe")
                        || normalizedMessage.contains(
                                "connection reset by peer")
                        || normalizedMessage.contains(
                                "connection aborted")
                        || normalizedMessage.contains(
                                "forcibly closed")
                        || normalizedMessage.contains(
                                "远程主机强迫关闭")
                        || normalizedMessage.contains(
                                "已中止一个已建立的连接")) {

                    return true;
                }
            }

            current = current.getCause();
            depth++;
        }

        return false;
    }
    
    /**
     * 记录图片访问完整请求指标。
     */
    private void recordAccessCompletion(
            String result,
            long requestStartNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.IMAGE_ACCESS_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.IMAGE_ACCESS_DURATION,
                elapsedMillis(requestStartNanos),
                tags);
    }

    /**
     * 记录流式输出阶段指标。
     */
    private void recordStreamMetrics(
            String result,
            long streamStartNanos,
            long transferredBytes) {

        metricRecorder.recordTimer(
                MetricName.IMAGE_ACCESS_STREAM_DURATION,
                elapsedMillis(streamStartNanos),
                MetricTags.builder()
                        .tag(MetricTag.RESULT, result)
                        .build());

        metricRecorder.recordValue(
                MetricName
                        .IMAGE_ACCESS_TRANSFERRED_BYTES,
                transferredBytes,
                MetricTags.empty());
    }

    /**
     * 记录预期响应文件大小。
     */
    private void recordResponseSize(
            Long fileSize) {

        if (fileSize == null
                || fileSize < 0L) {

            return;
        }

        metricRecorder.recordValue(
                MetricName.IMAGE_ACCESS_RESPONSE_SIZE,
                fileSize,
                MetricTags.empty());
    }

    /**
     * 流式输出失败是当前方法的最终异常责任边界。
     */
    private void recordStreamFailureSafely(
            String requestId,
            String traceId,
            String storagePath,
            long transferredBytes,
            Throwable throwable) {

        LogContext logContext =
                LogContext.builder()
                        .requestId(requestId)
                        .traceId(traceId)
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION_STREAM)
                        .eventName(
                                LogEventName
                                        .SYSTEM_EXCEPTION_OCCURRED)
                        .status(LogStatus.FAILED)
                        .errorCode(String.valueOf(
                                ImageAssetErrorCode
                                        .IMAGE_STREAM_FAILED
                                        .getCode()))
                        .message(
                                "image stream failed"
                                        + ", storagePath="
                                        + safeForLog(
                                                storagePath)
                                        + ", transferredBytes="
                                        + transferredBytes)
                        .build();

        try {
            logService.recordError(
                    logContext,
                    throwable);
        } catch (RuntimeException loggingException) {
            /*
             * 结构化日志自身失败时使用普通 Logger 兜底。
             */
            log.error(
                    "Image stream failed, storagePath={}, transferredBytes={}",
                    safeForLog(storagePath),
                    transferredBytes,
                    throwable);

            log.error(
                    "Failed to record image stream error",
                    loggingException);
        }
    }

    private String safeForLog(
            String value) {

        if (value == null) {
            return "";
        }

        String normalized = value
                .replace('\r', ' ')
                .replace('\n', ' ');

        if (normalized.length()
                <= MAX_LOG_PATH_LENGTH) {

            return normalized;
        }

        return normalized.substring(
                0,
                MAX_LOG_PATH_LENGTH);
    }

    private long elapsedMillis(
            long startNanos) {

        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

}
