package com.everypicfound.common.exception;

import lombok.Getter;

@Getter
public class BizException extends RuntimeException {

    private ErrorCode errorCode;//额外的错误码字段

    public ErrorCode getErrorCode() {
        throw new UnsupportedOperationException("TODO");
    }
}
