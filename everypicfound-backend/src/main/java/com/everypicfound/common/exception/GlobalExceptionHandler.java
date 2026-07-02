package com.everypicfound.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.response.Result;

import lombok.RequiredArgsConstructor;

/*
@RestControllerAdvice表示全局异常处理器，Controller 或者 Service 抛出的异常会被Handler拦截，并统一返回自己的fail Result
@ExceptionHandler()里面的类表示专门捕获对应类型的异常
这样就可以让controller和service里面专门写一大堆try-catch

<p>Controller 或同步 Service 链路中继续向上抛出的异常，
统一在此处记录一次并转换为标准响应。</p>
*/
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    /**
     * 仅在结构化日志系统自身发生异常时使用。
     *
     * <p>正常业务异常不得通过该 Logger 重复记录。</p>
     */
    private static final Logger FALLBACK_LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LogService logService;



    /**
     * 处理业务异常。
     *
     * <p>
     * 业务异常表示请求违反参数、状态或业务规则，
     * 记录为事件日志，不输出异常堆栈。
     * </p>
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        LogContext logContext = buildLogContext(
            LogEventName.BUSINESS_REQUEST_REJECTED,
            LogStatus.REJECTED,
            errorCode,
            errorCode.getMessage()
        );
        recordEventSafely(logContext);

        return buildFailResult(errorCode);
    }



    /**
     * 处理明确的系统异常。
     *
     * <p>
     * 系统异常通常来自数据库、文件系统、模型服务、
     * 向量数据库或其他基础设施，必须保留完整异常堆栈。
     * </p>
     */
    @ExceptionHandler(SystemException.class)
    public Result<Void> handleSystemException(SystemException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        
        LogContext logContext = buildLogContext(
                LogEventName.SYSTEM_EXCEPTION_OCCURRED,
                LogStatus.FAILED,
                errorCode,
                errorCode.getMessage());

        recordErrorSafely(logContext, exception);

        return buildFailResult(errorCode);

    }

    /**
     * 处理未被识别和转换的异常。
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnknownException(Exception exception) {
        ErrorCode errorCode = CommonErrorCode.SYSTEM_ERROR;

        LogContext logContext = buildLogContext(
                LogEventName.COMMON_UNHANDLED_EXCEPTION,
                LogStatus.FAILED,
                errorCode,
                "Unhandled exception: " + exception.getClass().getName());

        recordErrorSafely(logContext, exception);

        return buildFailResult(errorCode);
    }
    


    private LogContext buildLogContext(LogEventName eventName,
            LogStatus status,
            ErrorCode errorCode,
            String message) {
        return LogContext.builder()
                .eventName(eventName)
                .status(status)
                .errorCode(String.valueOf(errorCode.getCode()))
                .message(message)
                .build();
    }

    private Result<Void> buildFailResult(ErrorCode errorCode) {
        return Result.fail(
                errorCode.getCode(),
                errorCode.getMessage(),
                getRequestId());
    }

    /**
     * 防止日志模块自身异常改变原本应该返回给客户端的结果。
     */
    private void recordEventSafely(LogContext logContext) {
        try {
            logService.recordEvent(logContext);
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Failed to record structured event log, eventName="
                            + logContext.getEventName()
                            + ", errorCode="
                            + logContext.getErrorCode(),
                    loggingException);
        }
    }

    /**
     * 防止日志模块自身异常覆盖原始系统异常。
     */
    private void recordErrorSafely(LogContext logContext,
            Exception originalException) {
        try {
            logService.recordError(logContext, originalException);
        } catch (RuntimeException loggingException) {
            /*
             * 结构化日志写入失败时退回 ROOT Logger。
             * 第一条保留原始业务异常，第二条记录日志系统自身异常。
             */
            FALLBACK_LOGGER.error(
                    "Failed to record structured error log, eventName="
                            + logContext.getEventName()
                            + ", errorCode="
                            + logContext.getErrorCode(),
                    originalException);

            FALLBACK_LOGGER.error(
                    "Structured logging subsystem failure",
                    loggingException);
        }
    }

    private String getRequestId() {
        RequestContext context = RequestContextHolder.get();
        return context == null ? null : context.getRequestId();
    }

}
