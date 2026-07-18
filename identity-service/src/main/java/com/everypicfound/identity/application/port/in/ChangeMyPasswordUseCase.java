package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.ChangeMyPasswordCommand;

/**
 * 修改当前登录用户密码的用例入口。
 */
public interface ChangeMyPasswordUseCase {

    void changeMyPassword(ChangeMyPasswordCommand command);
}
