package com.everypicfound.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    
    SUCCESS(0, "success"),
                    
    PARAM_ERROR(400, "request params error"),
                    
    UNAUTHORIZED(401, "auth failed"),
                    
    FORBIDDEN(403, "forbidden"),

    NOT_FOUND(404, "resource not found"),
                    
    TOO_MANY_REQUEST(429, "request too busy"),
                    
    SYSTEM_ERROR(500, "system error"),
                    
    SERVICE_UNAVAILABLE(503, "service unavailable");

    private final Integer code;

    private final String message;
}
