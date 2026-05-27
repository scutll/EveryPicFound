package com.everypicfound.imageasset.application.service;

import org.springframework.stereotype.Service;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.result.ImageUploadResult;

@Service
public class DefaultImageAssetApplicationService implements ImageAssetApplicationService {
 
    @Override
    public ImageUploadResult upload(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
