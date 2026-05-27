package com.everypicfound.common.exception;

import lombok.Getter;
@Getter
public class SystemException extends RuntimeException {

    // 异常携带的错误码。
    private ErrorCode errorCode;

    // 获取异常携带的错误码。
    public ErrorCode getErrorCode() {
        throw new UnsupportedOperationException("TODO");
    }
}
