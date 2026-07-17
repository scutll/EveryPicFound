package com.everypicfound.identity.domain.model.session;

import java.time.Instant;
import java.util.Objects;

/**
 * 用户登录后在服务端持久化的会话。
 */
public final class UserSession {

    private final String sessionId;
    private final long userId;
    private final UserSessionStatus status;
    private final Instant createdAt;
    private final Instant expiresAt;

    private UserSession(
            String sessionId,
            long userId,
            UserSessionStatus status,
            Instant createdAt,
            Instant expiresAt) {
        requireText(sessionId, "sessionId");
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        this.sessionId = sessionId;
        this.userId = userId;
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(createdAt)) {
            throw new IllegalArgumentException(
                    "expiresAt must be after createdAt");
        }
    }

    public static UserSession create(
            String sessionId,
            long userId,
            Instant createdAt,
            Instant expiresAt) {
        return new UserSession(
                sessionId,
                userId,
                UserSessionStatus.ACTIVE,
                createdAt,
                expiresAt);
    }

    public String sessionId() {
        return sessionId;
    }

    public long userId() {
        return userId;
    }

    public UserSessionStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "UserSession[sessionId=" + sessionId + ", userId=" + userId
                + ", status=" + status + ", createdAt=" + createdAt
                + ", expiresAt=" + expiresAt + "]";
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
