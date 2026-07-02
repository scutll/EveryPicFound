package com.everypicfound.common.log;


/**
 * 日志上下文格式化策略
 */
public interface LogContextFormatter {
    /**
     * 将日志上下文转换为统一文本格式。
     *
     * @param context 日志上下文
     * @return 格式化后的日志文本
     */
    String format(LogContext context);
}
