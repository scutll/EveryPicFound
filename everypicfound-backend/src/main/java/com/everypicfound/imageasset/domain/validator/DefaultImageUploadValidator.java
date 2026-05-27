package com.everypicfound.imageasset.domain.validator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public class DefaultImageUploadValidator implements ImageUploadValidator {

    // 统一校验上传图片。
    @Override
    public void validate(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 校验文件不能为空。
    public void validateFileNotEmpty(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 校验文件大小。
    public void validateFileSize(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 校验文件扩展名。
    public void validateFileExt(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 校验 MIME 类型。
    public void validateMimeType(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    // 校验图片是否可解析。
    public void validateImageReadable(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
