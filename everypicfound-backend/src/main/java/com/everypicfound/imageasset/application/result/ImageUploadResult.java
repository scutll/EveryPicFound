package com.everypicfound.imageasset.application.result;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageUploadResult {
    
    private Long imageId;

    private String originalFileName;

    private String imageUrl;

    private ImageStatus imageStatus;

    private VectorStatus vectorStatus;
}
