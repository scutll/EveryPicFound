package com.everypicfound.identity.application.command;

/**
 * 退出当前登录会话命令。
 */
public record LogoutCurrentSessionCommand(long userId, String sessionId) {

    public LogoutCurrentSessionCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
    }
}
