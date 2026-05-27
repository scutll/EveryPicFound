package com.everypicfound.imageasset.domain.extractor;

import org.springframework.stereotype.Component;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

@Component
public class DefaultImageMetataExtractor implements ImageMetadataExtractor {
    
    @Override
    public ImageMetadata extract(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
