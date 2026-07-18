package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.DeleteMyAccountCommand;

/**
 * 注销当前用户账户的 HTTP 请求体。
 */
public record DeleteMyAccountRequest(String password) {

    public DeleteMyAccountCommand toCommand(long userId) {
        return new DeleteMyAccountCommand(userId, password);
    }
}
