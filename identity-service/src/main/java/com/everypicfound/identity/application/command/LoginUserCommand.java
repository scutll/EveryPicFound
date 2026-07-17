package com.everypicfound.identity.application.command;

/**
 * 用户名密码登录命令。
 */
public final class LoginUserCommand {

    private final String username;
    private final String rawPassword;

    public LoginUserCommand(String username, String rawPassword) {
        this.username = username;
        this.rawPassword = rawPassword;
    }

    public String username() {
        return username;
    }

    public String rawPassword() {
        return rawPassword;
    }

    @Override
    public String toString() {
        return "LoginUserCommand[username=" + username
                + ", rawPassword=PROTECTED]";
    }
}
