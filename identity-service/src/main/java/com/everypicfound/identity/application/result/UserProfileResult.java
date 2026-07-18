package com.everypicfound.identity.application.result;

import com.everypicfound.identity.domain.model.user.UserProfile;

/**
 * 当前用户资料用例返回值。
 */
public record UserProfileResult(
        long userId,
        String username,
        String nickname,
        String displayName,
        String avatarUrl) {

    public static UserProfileResult from(UserProfile profile) {
        return new UserProfileResult(
                profile.userId(),
                profile.username(),
                profile.nickname(),
                profile.displayName(),
                profile.avatarUrl());
    }
}
