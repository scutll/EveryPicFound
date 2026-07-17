package com.everypicfound.identity.application.exception;

/**
 * Refresh Token 缺失、过期、已使用、已撤销或所属会话无效。
 */
public final class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("refresh token is invalid");
    }
}
