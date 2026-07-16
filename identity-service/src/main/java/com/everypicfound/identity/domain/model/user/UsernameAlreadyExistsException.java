package com.everypicfound.identity.domain.model.user;

/**
 * 注册用户名已被占用。
 */
public final class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException() {
        super("username already exists");
    }

    public UsernameAlreadyExistsException(Throwable cause) {
        super("username already exists", cause);
    }
}
