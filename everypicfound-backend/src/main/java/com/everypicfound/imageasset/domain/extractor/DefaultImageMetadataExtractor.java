package com.everypicfound.imageasset.domain.extractor;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public class DefaultImageMetadataExtractor implements ImageMetadataExtractor {

    // 解析图片基础信息。
    @Override
    public ImageMetadata extract(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
