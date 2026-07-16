package com.everypicfound.identity.application.command;

/**
 * 用户注册应用命令。
 */
public final class RegisterUserCommand {

    private final String username;
    private final String rawPassword;
    private final String nickname;

    public RegisterUserCommand(
            String username,
            String rawPassword,
            String nickname) {
        this.username = username;
        this.rawPassword = rawPassword;
        this.nickname = nickname;
    }

    public String username() {
        return username;
    }

    /**
     * 返回待校验和哈希的原始密码，不得写入日志或响应。
     */
    public String rawPassword() {
        return rawPassword;
    }

    public String nickname() {
        return nickname;
    }

    @Override
    public String toString() {
        return "RegisterUserCommand[username=" + username
                + ", rawPassword=PROTECTED, nickname=" + nickname + "]";
    }
}
