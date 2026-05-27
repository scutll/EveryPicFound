package com.everypicfound.common.log;

import org.springframework.stereotype.Service;

@Service
public class Slf4jLogService implements LogService {
    
    @Override
    public void recordBizLog(LogContext context) {
    }

    @Override
    public void recordSuccessLog(LogContext context) {
    }

    @Override
    public void recordErrorLog(LogContext context) {
    }

    @Override
    public void recordStateChangeLog(LogContext context) {
    }

    @Override
    public void recordSlowLog(LogContext context) {
    }
}
