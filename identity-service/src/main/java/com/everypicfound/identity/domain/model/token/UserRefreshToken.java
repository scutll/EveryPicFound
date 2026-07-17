package com.everypicfound.identity.domain.model.token;

import java.time.Instant;
import java.util.Objects;

/**
 * 与用户会话绑定的 Refresh Token 持久化记录。
 */
public final class UserRefreshToken {

    private final String sessionId;
    private final String tokenHash;
    private final UserRefreshTokenStatus status;
    private final Instant issuedAt;
    private final Instant expiresAt;

    private UserRefreshToken(
            String sessionId,
            String tokenHash,
            UserRefreshTokenStatus status,
            Instant issuedAt,
            Instant expiresAt) {
        requireText(sessionId, "sessionId");
        requireText(tokenHash, "tokenHash");
        this.sessionId = sessionId;
        this.tokenHash = tokenHash;
        this.status = Objects.requireNonNull(status, "status");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after issuedAt");
        }
    }

    public static UserRefreshToken issue(
            String sessionId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt) {
        return new UserRefreshToken(
                sessionId,
                tokenHash,
                UserRefreshTokenStatus.ACTIVE,
                issuedAt,
                expiresAt);
    }

    public String sessionId() {
        return sessionId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UserRefreshTokenStatus status() {
        return status;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "UserRefreshToken[sessionId=" + sessionId
                + ", tokenHash=PROTECTED, status=" + status
                + ", issuedAt=" + issuedAt + ", expiresAt=" + expiresAt
                + "]";
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
