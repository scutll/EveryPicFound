package com.everypicfound.imageasset.domain.validator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import org.springframework.stereotype.Component;

@Component
public class DefaultImageUploadValidator implements ImageUploadValidator {

    @Override
    public void validate(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    public void validateFileNotEmpty(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    public void validateFileSize(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    public void validateFileExt(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    public void validateMimeType(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }

    public void validateImageReadable(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}