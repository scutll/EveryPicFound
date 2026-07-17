package com.everypicfound.identity.application.exception;

/**
 * 用户名密码认证失败；异常内容不得区分具体失败原因。
 */
public final class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid credentials");
    }
}
