package com.everypicfound.identity.application.command;

import com.everypicfound.identity.application.exception.InvalidRefreshTokenException;

/**
 * Refresh Token 换发命令。
 */
public final class RefreshTokenCommand {

    private final String refreshToken;

    public RefreshTokenCommand(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        this.refreshToken = refreshToken;
    }

    public String refreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        return "RefreshTokenCommand[refreshToken=PROTECTED]";
    }
}
