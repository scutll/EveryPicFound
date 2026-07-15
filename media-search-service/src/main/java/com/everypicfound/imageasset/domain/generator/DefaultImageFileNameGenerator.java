package com.everypicfound.imageasset.domain.generator;

import java.util.Locale;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;

@Component
public class DefaultImageFileNameGenerator implements ImageFileNameGenerator {

    private static final int MAX_FILE_EXT_LENGTH = 20;

    @Override
    // 生成图片文件名：把图片ID和文件扩展名拼接成文件名，并返回。
    public String generateFileName(Long imageId, String fileExt) {
        validateImageId(imageId);// 验证图片ID是否合法

        String normalizedFileExt = normalizeFileExt(fileExt);
        validateFileExt(normalizedFileExt);

        return imageId + "." + normalizedFileExt;
    }

    // 验证图片ID是否合法
    private void validateImageId(Long imageId) {
        if (imageId == null || imageId <= 0) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }
    }
    // 验证文件扩展名是否合法
    private String normalizeFileExt(String fileExt) {
        if (fileExt == null) {
            return "";
        }

        String normalized = fileExt.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }
    // 验证文件名是否合法
    private void validateFileExt(String fileExt) {
        if (fileExt.isBlank()) {
            throw new BizException(ImageAssetErrorCode.IMAGE_FORMAT_UNSUPPORTED);
        }

        if (fileExt.length() > MAX_FILE_EXT_LENGTH || !isAlphaNumeric(fileExt)) {
            throw new BizException(ImageAssetErrorCode.IMAGE_FORMAT_UNSUPPORTED);
        }
    }

    // 验证文件扩展名是否只包含字母和数字
    private boolean isAlphaNumeric(String fileExt) {
        for (int i = 0; i < fileExt.length(); i++) {
            char ch = fileExt.charAt(i);
            if (!isAsciiLetterOrDigit(ch)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiLetterOrDigit(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }
}
