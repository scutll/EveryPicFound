package com.everypicfound.identity.interfaces.rest.response;

import com.everypicfound.identity.application.result.LoginUserResult;

import java.time.Instant;

/**
 * 登录成功后的 Bearer Access Token 响应。
 */
public record LoginUserResponse(
        String accessToken,
        String tokenType,
        Instant expiresAt,
        String refreshToken,
        Instant refreshTokenExpiresAt) {

    public static LoginUserResponse from(LoginUserResult result) {
        return new LoginUserResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresAt(),
                result.refreshToken(),
                result.refreshTokenExpiresAt());
    }

    @Override
    public String toString() {
        return "LoginUserResponse[accessToken=PROTECTED, tokenType="
                + tokenType + ", expiresAt=" + expiresAt
                + ", refreshToken=PROTECTED, refreshTokenExpiresAt="
                + refreshTokenExpiresAt + "]";
    }
}
