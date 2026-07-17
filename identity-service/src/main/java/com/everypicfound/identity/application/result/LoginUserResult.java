package com.everypicfound.identity.application.result;

import java.time.Instant;
import java.util.Objects;

/**
 * 用户登录成功后返回给接口层的 Access Token 结果。
 */
public final class LoginUserResult {

    private static final String TOKEN_TYPE = "Bearer";

    private final String accessToken;
    private final Instant expiresAt;

    public LoginUserResult(String accessToken, Instant expiresAt) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException(
                    "accessToken must not be blank");
        }
        this.accessToken = accessToken;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
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

    @Override
    public String toString() {
        return "LoginUserResult[accessToken=PROTECTED, tokenType="
                + TOKEN_TYPE + ", expiresAt=" + expiresAt + "]";
    }
}
