package com.everypicfound.common.log;
public interface LogService {

    // 记录普通业务节点日志。
    void recordBizLog(LogContext context);

    // 记录成功节点日志。
    void recordSuccessLog(LogContext context);

    // 记录异常日志。
    void recordErrorLog(LogContext context);

    // 记录状态变更日志。
    void recordStateChangeLog(LogContext context);

    // 记录慢链路日志。
    void recordSlowLog(LogContext context);
}
