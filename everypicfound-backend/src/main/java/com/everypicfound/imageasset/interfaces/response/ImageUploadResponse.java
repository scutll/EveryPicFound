package com.everypicfound.imageasset.interfaces.response;

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
public class ImageUploadResponse {
    
    private long imageId;

    private String originalFileName;

    private String imageUrl;

    private ImageStatus imageStatus;

    private VectorStatus vectorStatus;


}