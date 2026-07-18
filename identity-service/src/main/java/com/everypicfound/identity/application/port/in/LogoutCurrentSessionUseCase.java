package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.LogoutCurrentSessionCommand;

/**
 * 退出当前登录会话用例。
 */
public interface LogoutCurrentSessionUseCase {

    void logout(LogoutCurrentSessionCommand command);
}
