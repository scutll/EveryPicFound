package com.everypicfound.identity.application.command;

/**
 * 当前登录用户注销自己账户的命令。
 */
public record DeleteMyAccountCommand(long userId, String password) {

    public DeleteMyAccountCommand {
        if (userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
    }
}
