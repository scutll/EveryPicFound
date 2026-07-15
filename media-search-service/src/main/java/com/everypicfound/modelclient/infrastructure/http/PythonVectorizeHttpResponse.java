package com.everypicfound.modelclient.infrastructure.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PythonVectorizeHttpResponse {
    
    private Boolean success;

    private String vectorizeType;

    private Long imageId;

    private List<Float> embedding;

    private Integer dim;

    private String modelName;

    private String errorCode;

    private String message;

    private Long costMs;


}
