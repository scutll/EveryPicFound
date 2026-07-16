package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.RegisterUserCommand;

/**
 * 注册接口请求；原始密码只允许向注册用例传递。
 */
public record RegisterUserRequest(
        String username,
        String password,
        String nickname) {

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(username, password, nickname);
    }

    @Override
    public String toString() {
        return "RegisterUserRequest[username=" + username
                + ", password=PROTECTED, nickname=" + nickname + "]";
    }
}
