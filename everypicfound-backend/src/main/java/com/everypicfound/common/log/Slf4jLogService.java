package com.everypicfound.common.log;
public class Slf4jLogService implements LogService {

    @Override
    // 记录普通业务节点日志。
    public void recordBizLog(LogContext context) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录成功节点日志。
    public void recordSuccessLog(LogContext context) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录异常日志。
    public void recordErrorLog(LogContext context) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录状态变更日志。
    public void recordStateChangeLog(LogContext context) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录慢链路日志。
    public void recordSlowLog(LogContext context) {
        throw new UnsupportedOperationException("TODO");
    }
}
