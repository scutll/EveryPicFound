package com.everypicfound.common.log;


/**
 * 系统统一日志门面。
 *
 * <p>
 * 业务模块只依赖本接口，不直接感知 Logger、Appender、
 * 文件路径以及同步或异步输出方式。
 * </p>
 */
public interface LogService {

    /**
     * 记录包含完整异常堆栈的系统错误。
     *
     * @param context   日志上下文
     * @param throwable 原始异常
     */
    void recordError(LogContext context, Throwable throwable);

    /**
     * 记录没有 Throwable，但存在明确系统失败结果的错误。
     *
     * @param context 日志上下文
     */
    void recordError(LogContext context);

    /**
     * 记录业务拒绝、重试、降级、状态变化或待补偿事件。
     *
     * @param context 日志上下文
     */
    void recordEvent(LogContext context);
    
}
