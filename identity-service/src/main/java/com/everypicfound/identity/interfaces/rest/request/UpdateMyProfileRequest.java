package com.everypicfound.identity.interfaces.rest.request;

import com.everypicfound.identity.application.command.UpdateMyProfileCommand;

/**
 * 修改当前用户基础资料的 HTTP 请求体。
 */
public record UpdateMyProfileRequest(
        String nickname,
        String avatarUrl) {

    public UpdateMyProfileCommand toCommand(long userId) {
        return new UpdateMyProfileCommand(userId, nickname, avatarUrl);
    }
}
