package com.everypicfound.identity.application.result;

/**
 * 注册成功后可返回给接口层的公开结果。
 */
public record RegisterUserResult(
        long userId,
        String username,
        String nickname,
        String displayName) {
}
