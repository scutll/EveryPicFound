package com.everypicfound.vectorization.infrastructure.publisher;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.everypicfound.common.executor.ExecutorBizType;
import com.everypicfound.common.executor.ExecutorProvider;
import com.everypicfound.common.executor.ThreadPoolManager;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;
import com.everypicfound.vectorization.application.processor.ImageVectorizationProcessor;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
/*
基于线程池的向量化任务发布器
成功失败都返回结果和发布结果细节，即作为线程池任务发布错误的最终处理边界
*/
public class ThreadPoolVectorizationTaskPublisher implements VectorizationTaskPublisher {

    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(ThreadPoolVectorizationTaskPublisher.class);

    private static final String MODULE = "vectorization";
    private static final String BIZ_TYPE = "IMAGE_VECTORIZATION";
    private static final String OPERATION = "publish";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";
    private static final String RESULT_INVALID = "invalid";

    private static final String SOURCE_IMAGE_UPLOAD = "image_upload";
    private static final String SOURCE_SCANNER = "scanner";
    private static final String SOURCE_MANUAL = "manual";
    private static final String SOURCE_UNKNOWN = "unknown";

    private final ExecutorProvider executorProvider;

    private final ImageVectorizationProcessor imageVectorizationProcessor;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    @Override
    public VectorizationPublishResult publish(ImageVectorizationTaskCommand command) {
        long startNanos = System.nanoTime();
        String source = resolveSource(command);
        String result = RESULT_FAILED;

        try {

            if (command == null || command.getImageId() == null) {

                result = RESULT_INVALID;
                recordPublishFailureSafely(command, VectorizationErrorCode.VECTORIZATION_TASK_INVALID, null);

                return publishFailed(command, VectorizationErrorCode.VECTORIZATION_TASK_INVALID);
            }

            Executor executor = executorProvider.getExecutor(ExecutorBizType.VECTORIZATION);
            // 在execute中检测到有decorator就会进行RequestContext透传
            executor.execute(() -> imageVectorizationProcessor.process(command));

            result = RESULT_SUCCESS;

            return VectorizationPublishResult.builder()
                    .success(true)
                    .imageId(command.getImageId())
                    .message("vectorization task submitted")
                    .build();
        } catch (RejectedExecutionException exception) {
            result = RESULT_REJECTED;

            metricRecorder.increment(MetricName.EXECUTOR_REJECTIONS,
                    MetricTags.builder().tag(MetricTag.EXECUTOR, ThreadPoolManager.COMMON_EXECUTOR_NAME).build());
            
            recordPublishFailureSafely(command, VectorizationErrorCode.VECTORIZATION_TASK_PUBLISH_FAILED, exception);

            return publishFailed(command, VectorizationErrorCode.VECTORIZATION_TASK_PUBLISH_FAILED);
        } catch (RuntimeException exception) {
            result = RESULT_FAILED;

            recordPublishFailureSafely(command, VectorizationErrorCode.VECTORIZATION_TASK_PUBLISH_FAILED, exception);
            return publishFailed(command, VectorizationErrorCode.VECTORIZATION_TASK_PUBLISH_FAILED);
        } finally {
            recordPublishMetrics(source, result, startNanos);
        }

    }

    private VectorizationPublishResult publishFailed(
            ImageVectorizationTaskCommand command,
            VectorizationErrorCode errorCode) {

        return VectorizationPublishResult.builder()
                .success(false)
                .imageId(command == null
                        ? null
                        : command.getImageId())
                .errorCode(errorCode)
                .message(errorCode.getMessage())
                .build();
    }

    private void recordPublishMetrics(
            String source,
            String result,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.SOURCE, source)
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.VECTORIZATION_PUBLISH_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.VECTORIZATION_PUBLISH_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private void recordPublishFailureSafely(
            ImageVectorizationTaskCommand command,
            VectorizationErrorCode errorCode,
            Throwable throwable) {

        LogContext context = LogContext.builder()
                .requestId(command == null
                        ? null
                        : command.getRequestId())
                .traceId(command == null
                        ? null
                        : command.getTraceId())
                .bizId(command == null
                        || command.getImageId() == null
                                ? null
                                : String.valueOf(command.getImageId()))
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION)
                .eventName(LogEventName.TASK_PUBLISH_FAILED)
                .status(LogStatus.FAILED)
                .errorCode(String.valueOf(errorCode.getCode()))
                .message(errorCode.getMessage())
                .build();

        try {
            if (throwable == null) {
                logService.recordError(context);
            } else {
                logService.recordError(context, throwable);
            }
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Failed to record vectorization publish error",
                    throwable == null
                            ? loggingException
                            : throwable);
        }
    }


    private String resolveSource(
            ImageVectorizationTaskCommand command) {

        if (command == null
                || command.getSource() == null
                || command.getSource().isBlank()) {
            return SOURCE_UNKNOWN;
        }

        String source = command.getSource()
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (source) {
            case SOURCE_IMAGE_UPLOAD -> SOURCE_IMAGE_UPLOAD;
            case SOURCE_SCANNER -> SOURCE_SCANNER;
            case SOURCE_MANUAL -> SOURCE_MANUAL;
            default -> SOURCE_UNKNOWN;
        };
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

}
