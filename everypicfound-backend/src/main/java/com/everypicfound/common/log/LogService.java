package com.everypicfound.common.log;

public interface LogService {

    void recordBizLog(LogContext context);

    void recordSuccessLog(LogContext context);

    void recordErrorLog(LogContext context);

    void recordStateChangeLog(LogContext context);

    void recordSlowLog(LogContext context);
    
}
