package com.everypicfound.identity.domain.model.user;

/**
 * 当前用户资料读取模型。
 */
public record UserProfile(
        long userId,
        String username,
        String nickname,
        String avatarUrl) {

    public UserProfile {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
    }

    public String displayName() {
        if (nickname == null || nickname.isBlank()) {
            return username;
        }
        return nickname;
    }
}
