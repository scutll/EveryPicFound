package com.everypicfound.identity.interfaces.rest.response;

import com.everypicfound.identity.application.result.RegisterUserResult;

/**
 * 注册成功后返回给客户端的公开账户信息。
 */
public record RegisterUserResponse(
        long userId,
        String username,
        String nickname,
        String displayName) {

    public static RegisterUserResponse from(RegisterUserResult result) {
        return new RegisterUserResponse(
                result.userId(),
                result.username(),
                result.nickname(),
                result.displayName());
    }
}
