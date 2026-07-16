package com.everypicfound.identity.domain.model.user;

/**
 * 注册原始密码无法满足领域规则时抛出的异常。
 */
public final class InvalidPasswordException
        extends IllegalArgumentException {

    private final PasswordViolation violation;

    public InvalidPasswordException(PasswordViolation violation) {
        super("Invalid password: " + violation);
        this.violation = violation;
    }

    public PasswordViolation violation() {
        return violation;
    }
}
