package com.everypicfound.vectorization.error;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VectorizationErrorCode implements ErrorCode {
    
    VECTORIZATION_TASK_INVALID(400001, "vectorization task invalid"),
                    
    VECTORIZATION_TASK_PUBLISH_FAILED(400002, "vectorization task publishment failed");

    private final Integer code;

    private final String message;
}
