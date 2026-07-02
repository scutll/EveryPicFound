package com.everypicfound.imageasset.domain.service;

import org.springframework.stereotype.Service;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.imageasset.domain.enums.CleanStatus;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 基于统一日志服务的孤儿文件记录服务。
 *
 * <p>
 * 文件已经保存，但图片元数据入库失败，并且文件删除补偿也失败时，
 * 记录孤儿文件事件，等待后续异步清理。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class LogOrphanFileLogService implements OrphanFileLogService {

    private static final String MODULE = "image-asset";

    private static final String BIZ_TYPE = "IMAGE_UPLOAD";

    private static final String OPERATION = "orphan-file-detected";

    private static final String DEFAULT_FAIL_REASON = "unknown";

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    @Override
    public void recordOrphanFile(OrphanFileRecord record) {

        /*
         * record 为空不是正常的孤儿文件事件，
         * 而是调用方传递了非法内部参数。
         */
        if (record == null) {
            recordInvalidRecordError();

            return;
        }

        /*
         * 只有确认存在孤儿文件时才增加该指标。
         */
        metricRecorder.increment(
                MetricName.ORPHAN_FILES,
                MetricTags.empty());

        RequestContext requestContext = RequestContextHolder.get();

        logService.recordEvent(
                LogContext.builder()
                        .requestId(requestContext == null
                                ? null
                                : requestContext.getRequestId())
                        .traceId(requestContext == null
                                ? null
                                : requestContext.getTraceId())
                        .bizId(record.getImageId() == null
                                ? null
                                : String.valueOf(
                                        record.getImageId()))
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName(
                                LogEventName.ORPHAN_FILE_DETECTED)
                        .status(LogStatus.WAITING)
                        .errorCode(String.valueOf(
                                ImageAssetErrorCode.ORPHAN_FILE_DELETE_FAILED
                                        .getCode()))
                        .message(buildMessage(record))
                        .build());

    }

    private String buildMessage(
            OrphanFileRecord record) {

        CleanStatus cleanStatus = record.getCleanStatus() == null
                ? CleanStatus.WAITING
                : record.getCleanStatus();

        int retryCount = record.getRetryCount() == null
                ? 0
                : record.getRetryCount();

        String failReason = isBlank(record.getFailReason())
                ? DEFAULT_FAIL_REASON
                : record.getFailReason();

        /*
         * storagePath 是后续清理孤儿文件所必需的信息，
         * 因此可以进入日志，但不能进入指标标签。
         */
        return "orphan file detected"
                + ", storagePath="
                + safe(record.getStoragePath())
                + ", accessUrl="
                + safe(record.getAccessUrl())
                + ", fileName="
                + safe(record.getFileName())
                + ", originalFileName="
                + safe(record.getOriginalFileName())
                + ", fileHash="
                + safe(record.getFileHash())
                + ", failReason="
                + safe(failReason)
                + ", retryCount="
                + retryCount
                + ", cleanStatus="
                + cleanStatus.name();
    }
    
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * 防止文件名等外部数据通过换行破坏单条日志结构。
     */
    private String safe(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).replace('\r', ' ').replace('\n',' ');
    }

    /**
     * record 为空说明程序内部调用不符合约定，
     * 此时记录系统错误，而不是孤儿文件事件。
     */
    private void recordInvalidRecordError() {
        RequestContext requestContext = RequestContextHolder.get();

        logService.recordError(
                LogContext.builder()
                        .requestId(requestContext == null
                                ? null
                                : requestContext.getRequestId())
                        .traceId(requestContext == null
                                ? null
                                : requestContext.getTraceId())
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName(
                                LogEventName.SYSTEM_EXCEPTION_OCCURRED)
                        .status(LogStatus.FAILED)
                        .errorCode(String.valueOf(
                                ImageAssetErrorCode.ORPHAN_FILE_DELETE_FAILED
                                        .getCode()))
                        .message(
                                "orphan file record is null")
                        .build());
    }


}
