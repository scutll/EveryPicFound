package com.everypicfound.identity.interfaces.rest.response;

import com.everypicfound.identity.application.result.UserProfileResult;

/**
 * 当前用户资料 HTTP 响应。
 */
public record UserProfileResponse(
        long userId,
        String username,
        String nickname,
        String displayName,
        String avatarUrl) {

    public static UserProfileResponse from(UserProfileResult result) {
        return new UserProfileResponse(
                result.userId(),
                result.username(),
                result.nickname(),
                result.displayName(),
                result.avatarUrl());
    }
}
