package com.everypicfound.identity.domain.model.user;

/**
 * 用户名无法满足领域规则时抛出的异常。
 */
public final class InvalidUsernameException
        extends IllegalArgumentException {

    private final UsernameViolation violation;

    public InvalidUsernameException(UsernameViolation violation) {
        super("Invalid username: " + violation);
        this.violation = violation;
    }

    public UsernameViolation violation() {
        return violation;
    }
}
