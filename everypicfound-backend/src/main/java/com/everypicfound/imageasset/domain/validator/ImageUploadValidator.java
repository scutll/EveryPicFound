package com.everypicfound.imageasset.domain.validator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public interface ImageUploadValidator {

    void validate(ImageUploadCommand command);
    
}
