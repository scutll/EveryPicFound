package com.everypicfound.identity.domain.model.token;

import com.everypicfound.identity.domain.model.session.UserSessionStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Refresh Token 及其所属 Session 的校验视图。
 */
public final class RefreshTokenSession {

    private final String tokenHash;
    private final UserRefreshTokenStatus tokenStatus;
    private final Instant tokenExpiresAt;
    private final String sessionId;
    private final long userId;
    private final UserSessionStatus sessionStatus;
    private final Instant sessionCreatedAt;
    private final Instant sessionExpiresAt;

    public RefreshTokenSession(
            String tokenHash,
            UserRefreshTokenStatus tokenStatus,
            Instant tokenExpiresAt,
            String sessionId,
            long userId,
            UserSessionStatus sessionStatus,
            Instant sessionCreatedAt,
            Instant sessionExpiresAt) {
        requireText(tokenHash, "tokenHash");
        requireText(sessionId, "sessionId");
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.tokenHash = tokenHash;
        this.tokenStatus = Objects.requireNonNull(
                tokenStatus,
                "tokenStatus");
        this.tokenExpiresAt = Objects.requireNonNull(
                tokenExpiresAt,
                "tokenExpiresAt");
        this.sessionId = sessionId;
        this.userId = userId;
        this.sessionStatus = Objects.requireNonNull(
                sessionStatus,
                "sessionStatus");
        this.sessionCreatedAt = Objects.requireNonNull(
                sessionCreatedAt,
                "sessionCreatedAt");
        this.sessionExpiresAt = Objects.requireNonNull(
                sessionExpiresAt,
                "sessionExpiresAt");
    }

    public String tokenHash() {
        return tokenHash;
    }

    public UserRefreshTokenStatus tokenStatus() {
        return tokenStatus;
    }

    public Instant tokenExpiresAt() {
        return tokenExpiresAt;
    }

    public String sessionId() {
        return sessionId;
    }

    public long userId() {
        return userId;
    }

    public UserSessionStatus sessionStatus() {
        return sessionStatus;
    }

    public Instant sessionCreatedAt() {
        return sessionCreatedAt;
    }

    public Instant sessionExpiresAt() {
        return sessionExpiresAt;
    }

    public boolean canRefreshAt(Instant now) {
        Objects.requireNonNull(now, "now");
        return tokenStatus == UserRefreshTokenStatus.ACTIVE
                && tokenExpiresAt.isAfter(now)
                && sessionStatus == UserSessionStatus.ACTIVE
                && sessionExpiresAt.isAfter(now);
    }

    @Override
    public String toString() {
        return "RefreshTokenSession[tokenHash=PROTECTED, tokenStatus="
                + tokenStatus + ", tokenExpiresAt=" + tokenExpiresAt
                + ", sessionId=" + sessionId + ", userId=" + userId
                + ", sessionStatus=" + sessionStatus
                + ", sessionCreatedAt=" + sessionCreatedAt
                + ", sessionExpiresAt=" + sessionExpiresAt + "]";
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
