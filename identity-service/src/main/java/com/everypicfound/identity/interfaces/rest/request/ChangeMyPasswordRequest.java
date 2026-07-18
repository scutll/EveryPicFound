package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.ChangeMyPasswordCommand;

/**
 * 修改当前用户密码的 HTTP 请求体。
 */
public record ChangeMyPasswordRequest(
        String currentPassword,
        String newPassword) {

    public ChangeMyPasswordCommand toCommand(long userId) {
        return new ChangeMyPasswordCommand(
                userId,
                currentPassword,
                newPassword);
    }
}
