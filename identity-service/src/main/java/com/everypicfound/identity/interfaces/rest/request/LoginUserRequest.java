package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.LoginUserCommand;

/**
 * 登录接口请求；原始密码只允许向登录用例传递。
 */
public record LoginUserRequest(String username, String password) {

    public LoginUserCommand toCommand() {
        return new LoginUserCommand(username, password);
    }

    @Override
    public String toString() {
        return "LoginUserRequest[username=" + username
                + ", password=PROTECTED]";
    }
}
