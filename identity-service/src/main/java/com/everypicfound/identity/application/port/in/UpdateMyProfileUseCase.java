package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.UpdateMyProfileCommand;
import com.everypicfound.identity.application.result.UserProfileResult;

/**
 * 当前登录用户修改自己的基础资料。
 */
public interface UpdateMyProfileUseCase {

    UserProfileResult updateMyProfile(UpdateMyProfileCommand command);
}
