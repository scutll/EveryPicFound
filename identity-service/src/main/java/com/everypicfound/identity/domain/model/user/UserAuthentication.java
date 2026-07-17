package com.everypicfound.identity.domain.model.user;

import com.everypicfound.identity.domain.enums.AccountStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * 登录用例读取的最小账户认证视图。
 */
public final class UserAuthentication {

    private final long userId;
    private final PasswordHash passwordHash;
    private final AccountStatus status;
    private final Instant authValidAfter;

    public UserAuthentication(
            long userId,
            PasswordHash passwordHash,
            AccountStatus status,
            Instant authValidAfter) {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.userId = userId;
        this.passwordHash = Objects.requireNonNull(
                passwordHash,
                "passwordHash");
        this.status = Objects.requireNonNull(status, "status");
        this.authValidAfter = Objects.requireNonNull(
                authValidAfter,
                "authValidAfter");
    }

    public long userId() {
        return userId;
    }

    public PasswordHash passwordHash() {
        return passwordHash;
    }

    public AccountStatus status() {
        return status;
    }

    public Instant authValidAfter() {
        return authValidAfter;
    }

    @Override
    public String toString() {
        return "UserAuthentication[userId=" + userId
                + ", passwordHash=PROTECTED, status=" + status
                + ", authValidAfter=" + authValidAfter + "]";
    }
}
