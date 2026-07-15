package com.everypicfound.imageasset.domain.extractor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageMetadata {

    private String fileExt;

    private String mimeType;

    private Integer width;

    private Integer height;

    private Long fileSize;
}