package com.everypicfound.common.exception;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.response.Result;

/*
@RestControllerAdvice表示全局异常处理器，Controller 或者 Service 抛出的异常会被Handler拦截，并统一返回自己的fail Result
@ExceptionHandler()里面的类表示专门捕获对应类型的异常
这样就可以让controller和service里面专门写一大堆try-catch
*/
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return Result.fail(errorCode.getCode(), errorCode.getMessage(), getRequestId());
    }

    @ExceptionHandler(SystemException.class)
    public Result<Void> handleSystemException(SystemException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return Result.fail(errorCode.getCode(), errorCode.getMessage(), getRequestId());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknownException(Exception exception) {
        return Result.fail(
                CommonErrorCode.SYSTEM_ERROR.getCode(),
                CommonErrorCode.SYSTEM_ERROR.getMessage(),
                getRequestId());
    }

    private String getRequestId() {
        RequestContext context = RequestContextHolder.get();
        return context == null? null : context.getRequestId();
    }
}
