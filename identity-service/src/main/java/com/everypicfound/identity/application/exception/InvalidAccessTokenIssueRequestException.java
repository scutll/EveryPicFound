package com.everypicfound.identity.application.exception;

/**
 * Access Token 签发请求违反内部契约。
 */
public final class InvalidAccessTokenIssueRequestException
        extends IllegalArgumentException {

    public InvalidAccessTokenIssueRequestException(String message) {
        super(message);
    }
}
