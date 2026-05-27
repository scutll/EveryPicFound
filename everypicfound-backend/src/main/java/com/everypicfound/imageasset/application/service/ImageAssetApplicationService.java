package com.everypicfound.imageasset.application.service;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.result.ImageUploadResult;

public interface ImageAssetApplicationService {

    ImageUploadResult upload(ImageUploadCommand command);
}
