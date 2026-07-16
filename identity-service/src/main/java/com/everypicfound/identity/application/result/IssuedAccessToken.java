package com.everypicfound.identity.application.result;

import java.time.Instant;
import java.util.Objects;

/**
 * 已签发 Access Token 的内部应用结果。
 */
public final class IssuedAccessToken {

    private final String tokenValue;
    private final Instant expiresAt;

    public IssuedAccessToken(String tokenValue, Instant expiresAt) {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException(
                    "tokenValue must not be blank");
        }
        this.tokenValue = tokenValue;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    /**
     * 返回 Bearer Token 原文；调用者不得记录或持久化该值。
     */
    public String tokenValue() {
        return tokenValue;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "IssuedAccessToken[tokenValue=PROTECTED, expiresAt="
                + expiresAt + "]";
    }
}
