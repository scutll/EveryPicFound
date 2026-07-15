package com.everypicfound.system.health;

import com.everypicfound.common.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统健康检查错误码。
 */
@Getter
@RequiredArgsConstructor
public enum SystemHealthErrorCode
        implements ErrorCode {

    MYSQL_UNHEALTHY(
            700001,
            "mysql health check failed"),

    QDRANT_UNHEALTHY(
            700002,
            "qdrant health check failed"),

    MODEL_SERVICE_UNHEALTHY(
            700003,
            "model service health check failed"),

    STARTUP_HEALTH_CHECK_FAILED(
            700004,
            "system startup health check failed"),

    HEALTH_CHECK_EXECUTION_FAILED(
            700005,
            "system health check execution failed");

    private final Integer code;

    private final String message;
}