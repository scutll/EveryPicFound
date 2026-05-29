package com.everypicfound.imageasset.domain.service;

import org.springframework.stereotype.Service;

import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.imageasset.domain.enums.CleanStatus;

import lombok.RequiredArgsConstructor;

/**
 * 基于统一日志服务的孤儿文件日志记录服务。
 *
 * <p>在文件已保存成功、图片元数据入库失败，并且补偿删除文件也失败时，
 * 将残留文件上下文记录到统一日志系统。</p>
 */
@Service
@RequiredArgsConstructor
public class LogOrphanFileLogService implements OrphanFileLogService {

    private static final String MODULE = "image-asset";

    private static final String BIZ_TYPE = "IMAGE_UPLOAD";

    private static final String OPERATION = "ORPHAN_FILE_RECORD";

    private static final String STATUS_FAILED = "FAILED";

    private static final String DEFAULT_FAIL_REASON = "UNKNOWN_ORPHAN_FILE_REASON";

    private final LogService logService;

    @Override
    public void recordOrphanFile(OrphanFileRecord record) {
        if (record == null) {
            recordNullOrphanFile();
            return;
        }

        CleanStatus cleanStatus = record.getCleanStatus() == null
                ? CleanStatus.WAITING
                : record.getCleanStatus();

        Integer retryCount = record.getRetryCount() == null
                ? 0
                : record.getRetryCount();

        String failReason = isBlank(record.getFailReason())
                ? DEFAULT_FAIL_REASON
                : record.getFailReason();

        String message = buildMessage(record, cleanStatus, retryCount, failReason);

        logService.recordErrorLog(LogContext.builder()
                .bizId(record.getImageId() == null ? null : String.valueOf(record.getImageId()))
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION)
                .eventName(LogEventName.ORPHAN_FILE_RECORD.name())
                .status(STATUS_FAILED)
                .message(message)
                .build());
    }

    private void recordNullOrphanFile() {
        logService.recordErrorLog(LogContext.builder()
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION)
                .eventName(LogEventName.ORPHAN_FILE_RECORD.name())
                .status(STATUS_FAILED)
                .message("orphan file record is null")
                .build());
    }

    private String buildMessage(
            OrphanFileRecord record,
            CleanStatus cleanStatus,
            Integer retryCount,
            String failReason) {
        return "orphan file detected"
                + ", imageId=" + safe(record.getImageId())
                + ", storagePath=" + safe(record.getStoragePath())
                + ", accessUrl=" + safe(record.getAccessUrl())
                + ", fileName=" + safe(record.getFileName())
                + ", originalFileName=" + safe(record.getOriginalFileName())
                + ", fileHash=" + safe(record.getFileHash())
                + ", failReason=" + safe(failReason)
                + ", retryCount=" + safe(retryCount)
                + ", cleanStatus=" + cleanStatus.name();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace("\r", " ")
                .replace("\n", " ");
    }
}
