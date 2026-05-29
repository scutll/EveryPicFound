package com.everypicfound.modelclient.domain;

import com.everypicfound.modelclient.domain.enums.VectorizeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.everypicfound.common.exception.ErrorCode;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorizeResult {
    
    private Boolean success;

    private VectorizeType vectorizeType;

    private Long imageId;

    private List<Float> embedding;

    private String modelName;

    private Integer dim;

    private ErrorCode errorCode;

    private String message;

    private Long costMs;


}
