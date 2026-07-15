package com.everypicfound.vectorization.domain.failure;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.service.ImageAssetStatusService;
import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultVectorizationFailureHandler
        implements VectorizationFailureHandler {

    private static final String MODULE = "vectorization";

    private static final String BIZ_TYPE = "IMAGE_VECTORIZATION";

    private static final String OPERATION_FILE_MISSING = "handle-file-missing";

    private static final String OPERATION_RETRY = "schedule-retry";

    private static final String OPERATION_DEAD_FAILURE = "handle-dead-failure";

    private final ImageAssetStatusService imageAssetStatusService;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    @Override
    public ImageVectorizationResult handleFileMissing(
            VectorizationFailureContext context) {

        Long imageId = getImageId(context);

        imageAssetStatusService.markInvalid(
                imageId,
                FailReason.FILE_NOT_FOUND);

        imageAssetStatusService.markVectorFailed(
                imageId,
                FailReason.FILE_NOT_FOUND);

        recordEventSafely(
                context,
                OPERATION_FILE_MISSING,
                LogEventName.IMAGE_FILE_MISSING,
                LogStatus.INVALIDATED,
                "image file missing and image asset invalidated");

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.FAILED)
                .failReason(FailReason.FILE_NOT_FOUND)
                .message(resolveMessage(
                        context,
                        VectorizationErrorCode.IMAGE_FILE_NOT_FOUND
                                .getMessage()))
                .build();
    }

    @Override
    public ImageVectorizationResult handleRetryableFailure(
            VectorizationFailureContext context) {

        Long imageId = getImageId(context);

        FailReason failReason = getFailReason(context);

        /*
         * 先增加 retry_count，再恢复为 PENDING，
         * 由扫描器或后续发布机制重新处理。
         */
        imageAssetStatusService.increaseRetryCount(imageId);

        imageAssetStatusService.markVectorPending(imageId);

        metricRecorder.increment(
                MetricName.VECTORIZATION_RETRIES,
                MetricTags.builder()
                        .tag(
                                MetricTag.REASON,
                                normalizeReason(failReason))
                        .build());

        recordEventSafely(
                context,
                OPERATION_RETRY,
                LogEventName.VECTORIZATION_RETRY_SCHEDULED,
                LogStatus.RETRYING,
                buildRetryMessage(
                        context,
                        failReason));

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.PENDING)
                .failReason(failReason)
                .message(resolveMessage(
                        context,
                        "vectorization retry scheduled"))
                .build();
    }

    @Override
    public ImageVectorizationResult handleDeadFailure(
            VectorizationFailureContext context) {

        Long imageId = getImageId(context);

        FailReason failReason = getFailReason(context);

        imageAssetStatusService.markVectorFailed(
                imageId,
                failReason);

        recordEventSafely(
                context,
                OPERATION_DEAD_FAILURE,
                LogEventName.VECTORIZATION_DEAD_FAILED,
                LogStatus.FAILED,
                buildDeadFailureMessage(
                        context,
                        failReason));

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.FAILED)
                .failReason(failReason)
                .message(resolveMessage(
                        context,
                        "vectorization entered final failed state"))
                .build();
    }

    /**
     * FailureHandler 只记录状态事件，不打印 context.cause。
     *
     * <p>
     * 原始 Throwable 已经由 Processor 统一记录，
     * 这里再次打印会形成重复错误日志。
     * </p>
     */
    private void recordEventSafely(
            VectorizationFailureContext context,
            String operation,
            LogEventName eventName,
            LogStatus status,
            String message) {

        FailReason failReason = getFailReason(context);

        LogContext logContext = LogContext.builder()
                .requestId(context.getRequestId())
                .traceId(context.getTraceId())
                .bizId(String.valueOf(
                        context.getImageId()))
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(operation)
                .eventName(eventName)
                .status(status)
                .errorCode(failReason.name())
                .message(message)
                .build();

        try {
            logService.recordEvent(logContext);
        } catch (RuntimeException loggingException) {
            /*
             * 观测系统失败不能覆盖已经完成的状态更新。
             */
            log.error(
                    "Failed to record vectorization failure event, eventName={}",
                    eventName,
                    loggingException);
        }
    }

    private String buildRetryMessage(
            VectorizationFailureContext context,
            FailReason failReason) {

        return "vectorization retry scheduled"
                + ", reason="
                + normalizeReason(failReason)
                + ", retryCount="
                + resolveNextRetryCount(context)
                + ", maxRetryCount="
                + resolveMaxRetryCount(context);
    }

    private String buildDeadFailureMessage(
            VectorizationFailureContext context,
            FailReason failReason) {

        return "vectorization entered final failed state"
                + ", reason="
                + normalizeReason(failReason)
                + ", retryCount="
                + resolveRetryCount(context)
                + ", maxRetryCount="
                + resolveMaxRetryCount(context);
    }

    private Long getImageId(
            VectorizationFailureContext context) {

        if (context == null
                || context.getImageId() == null) {

            throw new SystemException(
                    VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
        }

        return context.getImageId();
    }

    private FailReason getFailReason(
            VectorizationFailureContext context) {

        if (context == null
                || context.getFailReason() == null) {

            return FailReason.MODEL_SERVICE_ERROR;
        }

        return context.getFailReason();
    }

    private int resolveRetryCount(
            VectorizationFailureContext context) {

        if (context == null
                || context.getRetryCount() == null
                || context.getRetryCount() < 0) {

            return 0;
        }

        return context.getRetryCount();
    }

    private int resolveNextRetryCount(
            VectorizationFailureContext context) {

        return resolveRetryCount(context) + 1;
    }

    private int resolveMaxRetryCount(
            VectorizationFailureContext context) {

        if (context == null
                || context.getMaxRetryCount() == null
                || context.getMaxRetryCount() < 0) {

            return 0;
        }

        return context.getMaxRetryCount();
    }

    private String resolveMessage(
            VectorizationFailureContext context,
            String defaultMessage) {

        if (context == null
                || context.getErrorMessage() == null
                || context.getErrorMessage().isBlank()) {

            return defaultMessage;
        }

        return context.getErrorMessage();
    }

    private String normalizeReason(
            FailReason failReason) {

        if (failReason == null) {
            return "unknown";
        }

        return failReason.name()
                .toLowerCase(Locale.ROOT);
    }
}