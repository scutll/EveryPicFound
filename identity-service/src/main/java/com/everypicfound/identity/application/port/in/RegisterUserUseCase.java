package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.RegisterUserCommand;
import com.everypicfound.identity.application.result.RegisterUserResult;

/**
 * 注册新用户用例。
 */
public interface RegisterUserUseCase {

    RegisterUserResult register(RegisterUserCommand command);
}
