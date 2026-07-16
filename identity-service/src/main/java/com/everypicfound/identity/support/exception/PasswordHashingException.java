package com.everypicfound.identity.support.exception;

/**
 * 密码哈希基础设施执行失败。
 */
public final class PasswordHashingException extends RuntimeException {

    public PasswordHashingException(String message, Throwable cause) {
        super(message, cause);
    }
}
