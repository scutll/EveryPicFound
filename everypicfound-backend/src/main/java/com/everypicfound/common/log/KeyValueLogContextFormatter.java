package com.everypicfound.common.log;

import org.springframework.stereotype.Component;

@Component
public class KeyValueLogContextFormatter implements LogContextFormatter {
    
    private static final String EMPTY_VALUE = "";

    @Override
    public String format(LogContext context) {
        LogContext safeContext = context == null
                ? LogContext.builder()
                        .message("log context null")
                        .build()
                : context;

        return "requestId=" + sanitize(safeContext.getRequestId())
                + ", traceId=" + sanitize(safeContext.getTraceId())
                + ", bizId=" + sanitize(safeContext.getBizId())
                + ", bizType=" + sanitize(safeContext.getBizType())
                + ", module=" + sanitize(safeContext.getModule())
                + ", operation=" + sanitize(safeContext.getOperation())
                + ", eventName=" + enumName(safeContext.getEventName())
                + ", status=" + enumName(safeContext.getStatus())
                + ", costMs=" + numberValue(safeContext.getCostMs())
                + ", errorCode=" + sanitize(safeContext.getErrorCode())
                + ", message=" + sanitize(safeContext.getMessage());
    }
    
    private String enumName(Enum<?> value){
        return value == null? EMPTY_VALUE: value.name();
    }

    private String numberValue(Number value) {
        return value == null ? EMPTY_VALUE : String.valueOf(value);
    }
    
    //删除换行字符
    private String sanitize(String value) {
        if (value == null) {
            return EMPTY_VALUE;
        }

        return value.replace('\r', ' ').replace('\n', ' ');
    }


}
