package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.LoginUserCommand;
import com.everypicfound.identity.application.result.LoginUserResult;

/**
 * 用户名密码登录用例。
 */
public interface LoginUserUseCase {

    LoginUserResult login(LoginUserCommand command);
}
