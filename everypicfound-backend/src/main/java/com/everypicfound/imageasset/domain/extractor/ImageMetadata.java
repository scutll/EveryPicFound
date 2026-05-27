package com.everypicfound.imageasset.domain.extractor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {

    // 文件扩展名。
    private String fileExt;

    // MIME 类型。
    private String mimeType;

    // 图片宽度。
    private Integer width;

    // 图片高度。
    private Integer height;

    // 文件大小。
    private Long fileSize;
}
