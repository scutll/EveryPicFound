package com.everypicfound.identity.application.result;

import java.time.Instant;
import java.util.Objects;

/**
 * Refresh Token 换发成功后返回给接口层的 Token 对。
 */
public final class RefreshTokenResult {

    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final Instant expiresAt;
    private final String refreshToken;
    private final Instant refreshTokenExpiresAt;

    public RefreshTokenResult(
            String accessToken,
            Instant expiresAt,
            String refreshToken,
            Instant refreshTokenExpiresAt) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "accessToken must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException(
                    "refreshToken must not be blank");
        }
        this.accessToken = accessToken;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        this.refreshToken = refreshToken;
        this.refreshTokenExpiresAt = Objects.requireNonNull(
                refreshTokenExpiresAt,
                "refreshTokenExpiresAt");
    }

    public String accessToken() {
        return accessToken;
    }

    public String tokenType() {
        return TOKEN_TYPE;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public String refreshToken() {
        return refreshToken;
    }

    public Instant refreshTokenExpiresAt() {
        return refreshTokenExpiresAt;
    }

    @Override
    public String toString() {
        return "RefreshTokenResult[accessToken=PROTECTED, tokenType="
                + TOKEN_TYPE + ", expiresAt=" + expiresAt
                + ", refreshToken=PROTECTED, refreshTokenExpiresAt="
                + refreshTokenExpiresAt + "]";
    }
}
