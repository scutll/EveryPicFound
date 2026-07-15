package com.everypicfound.common.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class Slf4jLogService implements LogService {

    private static final String UNKNOWN_VALUE = "unknown";

    // 不同日志通道使用独立appender
    /**
     * 固定错误日志通道。
     */
    private static final Logger ERROR_LOGGER = LoggerFactory.getLogger("EPF_ERROR");

    /**
     * 固定事件日志通道。
     */
    private static final Logger EVENT_LOGGER = LoggerFactory.getLogger("EPF_EVENT");


    private final LogContextFormatter logContextFormatter;

    private final LogProperties logProperties;


    @Override
    public void recordError(LogContext context, Throwable throwable) {
        LogContext enrichedContext = enrichContext(context);
        String formattedContext = logContextFormatter.format(enrichedContext);

        if (throwable == null) {
            ERROR_LOGGER.error(formattedContext);
            return;
        }

        ERROR_LOGGER.error(formattedContext, throwable);
    }

    @Override
    public void recordError(LogContext context) {
        LogContext enrichedContext = enrichContext(context);
        ERROR_LOGGER.error(logContextFormatter.format(enrichedContext));
    }

    @Override
    public void recordEvent(LogContext context) {
        if (!logProperties.isEventEnabled()) {
            return;
        }

        LogContext enrichedContext = enrichContext(context);
        EVENT_LOGGER.warn(logContextFormatter.format(enrichedContext));
    }

    /**
     * 使用当前线程的 RequestContext 补齐调用方未提供的字段。
     *
     * <p>
     * 优先使用调用方显式传入的值，其次使用 RequestContext 中的值。
     * </p>
     */
    private LogContext enrichContext(LogContext context) {
        RequestContext requestContext = RequestContextHolder.get();

        LogContext source = context == null
                ? LogContext.builder()
                        .message("log context is null")
                        .build()
                : context;

        return source.toBuilder()
                .requestId(firstNonBlank(
                        source.getRequestId(),
                        requestContext == null ? null : requestContext.getRequestId()))
                .traceId(firstNonBlank(
                        source.getTraceId(),
                        requestContext == null ? null : requestContext.getTraceId()))
                .bizId(firstNonBlank(
                        source.getBizId(),
                        requestContext == null ? null : requestContext.getBizId()))
                .module(firstNonBlank(
                        source.getModule(),
                        requestContext == null
                                ? UNKNOWN_VALUE
                                : requestContext.getModule()))
                .operation(firstNonBlank(
                        source.getOperation(),
                        requestContext == null
                                ? UNKNOWN_VALUE
                                : requestContext.getOperation()))
                .build();
    }


    private String firstNonBlank(String primary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }

        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }

        return null;
    }

}
