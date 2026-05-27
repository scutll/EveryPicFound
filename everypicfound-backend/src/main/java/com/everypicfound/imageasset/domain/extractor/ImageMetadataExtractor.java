package com.everypicfound.imageasset.domain.extractor;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public interface ImageMetadataExtractor {

    // 从图片文件中解析宽高、类型等信息。
    ImageMetadata extract(ImageUploadCommand command);
}
