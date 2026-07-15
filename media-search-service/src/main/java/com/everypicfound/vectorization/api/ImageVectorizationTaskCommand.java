package com.everypicfound.vectorization.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageVectorizationTaskCommand {
    
    private Long imageId;

    private String traceId;

    private String requestId;

    private String source;
}
