package com.everypicfound.identity.domain.repository;

import com.everypicfound.identity.domain.model.token.UserRefreshToken;

/**
 * Refresh Token 仓储。
 */
public interface UserRefreshTokenRepository {

    void save(UserRefreshToken refreshToken);
}
