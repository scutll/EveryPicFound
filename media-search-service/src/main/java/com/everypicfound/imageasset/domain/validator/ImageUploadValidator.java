package com.everypicfound.imageasset.domain.validator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public interface ImageUploadValidator {

    // 执行上传前完整校验。
    void validate(ImageUploadCommand command);
}
