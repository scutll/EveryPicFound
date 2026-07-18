package com.everypicfound.identity.application.command;

/**
 * 当前登录用户修改自己基础资料的命令。
 */
public record UpdateMyProfileCommand(
        long userId,
        String nickname,
        String avatarUrl) {

    public UpdateMyProfileCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
