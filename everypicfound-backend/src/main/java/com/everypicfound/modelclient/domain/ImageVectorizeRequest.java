package com.everypicfound.modelclient.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.everypicfound.modelclient.domain.enums.ImageInputType;

import java.io.InputStream;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ImageVectorizeRequest {
    
    private ImageInputType ImageInputType;

    private Long imageId;

    private String storagePath;

    private InputStream inputStream;

    private String originalFileName;

    private Long fileSize;

    private String mimeType;

    private String modelName;

    private String traceId;

    private String requestId;

}
