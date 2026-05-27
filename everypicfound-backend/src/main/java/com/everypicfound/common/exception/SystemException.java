package com.everypicfound.common.exception;

import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {
    
    public final ErrorCode errorCode;

    public SystemException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
