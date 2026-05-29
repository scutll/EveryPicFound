package com.everypicfound.common.log;

import org.springframework.stereotype.Component;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Slf4jLogService implements LogService {

    @Override
    // 记录普通业务节点日志。
    public void recordBizLog(LogContext context) {
        log.info("bizLog {}", formatContext(context));
    }

    @Override
    // 记录成功节点日志。
    public void recordSuccessLog(LogContext context) {
        log.info("successLog {}", formatContext(context));
    }

    @Override
    // 记录异常日志。
    public void recordErrorLog(LogContext context) {
        log.error("errorLog {}", formatContext(context));
    }

    @Override
    // 记录状态变更日志。
    public void recordStateChangeLog(LogContext context) {
        log.info("stateChangeLog {}", formatContext(context));
    }

    @Override
    // 记录慢链路日志。
    public void recordSlowLog(LogContext context) {
        log.warn("slowLog {}", formatContext(context));
    }

    // 格式化日志上下文，统一输出 requestId、traceId、bizId 等排查字段。
    private String formatContext(LogContext context) {
        RequestContext requestContext = RequestContextHolder.get();
        if (context == null) {
            return "traceId=" + safe(requestContext == null ? null : requestContext.getTraceId())
                    + ", requestId=" + safe(requestContext == null ? null : requestContext.getRequestId())
                    + ", module=" + safe(requestContext == null ? null : requestContext.getModule())
                    + ", bizType=, bizId=" + safe(requestContext == null ? null : requestContext.getBizId())
                    + ", eventName=, status=, operation=" + safe(requestContext == null ? null : requestContext.getOperation())
                    + ", costMs=, errorCode=, message=log context is null";
        }// codex: 如果调用方传入的 context 为空，则从 RequestContext 中提取 traceId、requestId、module、bizId、operation 等字段，并在日志中注明 "log context is null"，以便排查日志上下文缺失问题。

        return "traceId=" + safe(firstNonBlank(context.getTraceId(), requestContext == null ? null : requestContext.getTraceId()))
                + ", requestId=" + safe(firstNonBlank(context.getRequestId(), requestContext == null ? null : requestContext.getRequestId()))
                + ", module=" + safe(firstNonBlank(context.getModule(), requestContext == null ? null : requestContext.getModule()))
                + ", bizType=" + safe(context.getBizType())
                + ", bizId=" + safe(firstNonBlank(context.getBizId(), requestContext == null ? null : requestContext.getBizId()))
                + ", eventName=" + safe(context.getEventName())
                + ", status=" + safe(context.getStatus())
                + ", operation=" + safe(firstNonBlank(context.getOperation(), requestContext == null ? null : requestContext.getOperation()))
                + ", costMs=" + safeNumber(context.getCostMs())
                + ", errorCode=" + safe(context.getErrorCode())
                + ", message=" + safe(context.getMessage());
    }// codex: 该方法会优先使用 context 中的 traceId、requestId、module、bizId、operation 等字段，如果这些字段为空，则回退到 RequestContext 中对应的字段，确保日志中尽可能包含完整的上下文信息，方便问题排查。

    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        return fallback;
    }

    private String safeNumber(Number value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ");
    }
}
