package com.everypicfound.modelclient.infrastructure.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonHealthHttpResponse {
    
    private Boolean success;

    /*
    服务状态
    */
    private String status;

    private Boolean modelLoaded;

    private String modelName;

    private Integer vectorDim;

    private String device;

    private String errorCode;

    private String message;

}
