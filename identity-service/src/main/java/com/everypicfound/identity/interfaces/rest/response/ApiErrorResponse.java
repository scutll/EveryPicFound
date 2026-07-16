package com.everypicfound.identity.interfaces.rest.response;

/**
 * Identity Service 对客户端公开的字段级错误响应。
 */
public record ApiErrorResponse(
        String errorCode,
        String message,
        String field) {
}
