package com.everypicfound.identity.application.command;

/**
 * 当前登录用户修改自己密码的命令。
 */
public record ChangeMyPasswordCommand(
        long userId,
        String currentPassword,
        String newPassword) {

    public ChangeMyPasswordCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
