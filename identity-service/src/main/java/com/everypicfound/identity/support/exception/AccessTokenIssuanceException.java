package com.everypicfound.identity.support.exception;

/**
 * Access Token 基础设施签发失败。
 */
public final class AccessTokenIssuanceException extends RuntimeException {

    public AccessTokenIssuanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
