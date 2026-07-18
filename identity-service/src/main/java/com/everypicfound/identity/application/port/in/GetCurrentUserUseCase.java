package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.result.UserProfileResult;

/**
 * 查询当前登录用户资料。
 */
public interface GetCurrentUserUseCase {

    UserProfileResult getCurrentUser(long userId);
}
