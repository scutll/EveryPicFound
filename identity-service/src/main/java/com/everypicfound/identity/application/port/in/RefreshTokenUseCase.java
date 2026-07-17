package com.everypicfound.identity.application.port.in;

import com.everypicfound.identity.application.command.RefreshTokenCommand;
import com.everypicfound.identity.application.result.RefreshTokenResult;

/**
 * 使用 Refresh Token 换发新 Token 对的用例。
 */
public interface RefreshTokenUseCase {

    RefreshTokenResult refresh(RefreshTokenCommand command);
}
