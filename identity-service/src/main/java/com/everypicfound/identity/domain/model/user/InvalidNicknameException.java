package com.everypicfound.identity.domain.model.user;

/**
 * 昵称无法满足领域规则时抛出的异常。
 */
public final class InvalidNicknameException
        extends IllegalArgumentException {

    private final NicknameViolation violation;

    public InvalidNicknameException(NicknameViolation violation) {
        super("Invalid nickname: " + violation);
        this.violation = violation;
    }

    public NicknameViolation violation() {
        return violation;
    }
}
