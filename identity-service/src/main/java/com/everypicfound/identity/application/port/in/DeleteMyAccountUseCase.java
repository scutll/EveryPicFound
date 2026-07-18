package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.DeleteMyAccountCommand;

/**
 * 注销当前登录用户账户的用例入口。
 */
public interface DeleteMyAccountUseCase {

    void deleteMyAccount(DeleteMyAccountCommand command);
}
