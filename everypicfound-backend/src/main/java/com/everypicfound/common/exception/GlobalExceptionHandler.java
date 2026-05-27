package com.everypicfound.common.exception;

import com.everypicfound.common.response.Result;
public class GlobalExceptionHandler {

    // 处理业务异常，统一转换为 Result。
    public Result<Void> handleBizException(BizException exception) {
        throw new UnsupportedOperationException("TODO");
    }

    // 处理系统异常，统一转换为 Result。
    public Result<Void> handleSystemException(SystemException exception) {
        throw new UnsupportedOperationException("TODO");
    }

    // 处理未知异常，统一转换为 Result。
    public Result<Void> handleUnknownException(Exception exception) {
        throw new UnsupportedOperationException("TODO");
    }
}
