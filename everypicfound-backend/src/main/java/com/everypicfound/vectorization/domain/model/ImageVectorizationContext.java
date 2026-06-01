package com.everypicfound.vectorization.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageVectorizationContext {
    
    private Long imageId;

    private String traceId;

    private String requestId;

    private String storagePath;

    private String originalFileName;

    private String fileExt;

    private String mimeType;

    private Long fileSize;

    private Integer retryCount;
}
