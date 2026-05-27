package com.everypicfound.imageasset.domain.extractor;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public interface ImageMetadataExtractor {
    
    ImageMetadata extract(ImageUploadCommand command);
}
