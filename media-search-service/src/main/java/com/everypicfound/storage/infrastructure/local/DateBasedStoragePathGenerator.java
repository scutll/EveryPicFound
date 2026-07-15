package com.everypicfound.storage.infrastructure.local;

import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.SystemException;
import com.everypicfound.storage.core.StoragePathContext;
import com.everypicfound.storage.core.StoragePathGenerator;
import com.everypicfound.storage.error.StorageErrorCode;

@Component
public class DateBasedStoragePathGenerator implements StoragePathGenerator {

    @Override
    public String generatePath(StoragePathContext context) {
        validateContext(context);

        LocalDateTime uploadTime = context.getUploadTime();
        String fileExt = normalizeFileExt(context.getFileExt());

        return String.format(Locale.ROOT, "%04d/%02d/%02d/%d.%s",
                uploadTime.getYear(),
                uploadTime.getMonthValue(),
                uploadTime.getDayOfMonth(),
                context.getImageId(),
                fileExt);
                // codex: 生成的路径格式为 "yyyy/MM/dd/imageId.ext"，例如 "2024/06/18/12345.jpg"，方便按日期分层存储和管理。
    }

    // 验证上下文参数
    private void validateContext(StoragePathContext context) {
        if (context == null
                || context.getImageId() == null
                || context.getImageId() <= 0
                || context.getUploadTime() == null
                || context.getFileExt() == null
                || context.getFileExt().isBlank()) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }
    }
// 规范化文件扩展名，去除点号并转换为小写，同时验证合法性
    private String normalizeFileExt(String fileExt) {
        String normalized = fileExt.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        if (normalized.isBlank() || !isAlphaNumeric(normalized)) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }
        return normalized;
    }
// 验证字符串是否只包含字母和数字
    private boolean isAlphaNumeric(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isLetterOrDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
