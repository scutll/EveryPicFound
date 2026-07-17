package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.RefreshTokenCommand;

/**
 * Refresh Token 换发请求。
 */
public record RefreshTokenRequest(String refreshToken) {

    public RefreshTokenCommand toCommand() {
        return new RefreshTokenCommand(refreshToken);
    }

    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=PROTECTED]";
    }
}
