package com.everypicfound.system.health;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;

import lombok.RequiredArgsConstructor;

/**
 * 应用启动健康检查。
 */
@Component
@RequiredArgsConstructor
public class StartupHealthCheckHook implements ApplicationRunner {
    
    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(
            StartupHealthCheckHook.class);

    private static final String MODULE = "system-health";

    private static final String BIZ_TYPE = "SYSTEM_HEALTH";

    private static final String OPERATION = "startup-health-check";
    
    private final SystemHealthCheckService systemHealthCheckService;

    private final SystemHealthProperties properties;

    private final LogService logService;

    @Override
    public void run(ApplicationArguments args) {
        SystemHealthSnapshot snapshot = systemHealthCheckService.check(
                true,
                HealthCheckSource.STARTUP);

        boolean failFastEnabled = Boolean.TRUE.equals(
                properties.getStartupFailFast());

        boolean healthy = snapshot != null
                && Boolean.TRUE.equals(
                        snapshot.getHealthy());
       
        if (!failFastEnabled || healthy) {
            return;
        }

        SystemException exception = new SystemException(SystemHealthErrorCode.STARTUP_HEALTH_CHECK_FAILED);

        /*
         * ApplicationRunner 抛出的异常不会经过 HTTP
         * GlobalExceptionHandler，因此当前 Hook 是最终异常责任点。
         */
        recordStartupFailureSafely(snapshot, exception);

        throw exception;
    }

    private void recordStartupFailureSafely(
            SystemHealthSnapshot snapshot, SystemException exception) {

        String unhealthySummary = buildUnhealthySummary(snapshot);

        String errorCode = String.valueOf(SystemHealthErrorCode.STARTUP_HEALTH_CHECK_FAILED.getCode());

        LogContext context =
                LogContext.builder()
                        .bizType(BIZ_TYPE)
                        .module(MODULE)
                        .operation(OPERATION)
                        .eventName(
                                LogEventName
                                        .SYSTEM_EXCEPTION_OCCURRED)
                        .status(LogStatus.FAILED)
                        .errorCode(errorCode)
                        .message(
                                "startup health check failed"
                                        + ", unhealthyComponents="
                                        + buildUnhealthySummary(
                                                snapshot))
                        .build();

        try {
            /*
             * 启动异常不会继续进入 GlobalExceptionHandler，
             * 因此这里记录唯一一次异常堆栈。
             */
            logService.recordError(context, exception);
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Startup health check failed, unhealthyComponents={}",
                    unhealthySummary, exception);

            FALLBACK_LOGGER.error(
                    "Failed to record startup health check error",
                    loggingException);
        }

        LogContext eventContext = LogContext.builder()
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION)
                .eventName(
                        LogEventName.STARTUP_HEALTH_CHECK_FAILED)
                .status(LogStatus.FAILED)
                .errorCode(errorCode)
                .message(
                        "startup health check failed"
                                + ", failFastEnabled=true"
                                + ", unhealthyComponents="
                                + unhealthySummary)
                .build();

        try {
            /*
             * 这是低频状态事件，用于表达启动检查失败并触发 fail-fast。
             * 它不携带 Throwable，不重复打印异常栈。
             */
            logService.recordEvent(eventContext);
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Failed to record startup health check event, unhealthyComponents={}",
                    unhealthySummary,
                    loggingException);
        }
    }


    private String buildUnhealthySummary(
            SystemHealthSnapshot snapshot) {

        if (snapshot == null
                || snapshot.getComponents() == null) {

            return "unknown";
        }

        String summary = snapshot.getComponents()
                .stream()
                .filter(component ->
                        !Boolean.TRUE.equals(
                                component.getHealthy()))
                .map(component ->
                        component.getComponent()
                                + ":"
                                + component.getMessage())
                .collect(Collectors.joining(","));

        return summary.isBlank()
                ? "unknown"
                : summary;
    }
}
