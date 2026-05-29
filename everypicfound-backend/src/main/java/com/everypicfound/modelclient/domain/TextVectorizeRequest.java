package com.everypicfound.modelclient.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TextVectorizeRequest {
    private String text;

    private String modelName;

    private String traceId;

    private String requestId;
    
}
