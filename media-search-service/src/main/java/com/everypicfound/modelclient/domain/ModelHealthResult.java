package com.everypicfound.modelclient.domain;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ModelHealthResult {
    
    private Boolean success;

    /**
     * 服务状态，例如 UP / DOWN。
     */
    private String status;

    private Boolean modelLoaded;

    private String modelName;

    private Integer vectorDim;

    private String device;

    private ErrorCode errorCode;

    private String message;
}
