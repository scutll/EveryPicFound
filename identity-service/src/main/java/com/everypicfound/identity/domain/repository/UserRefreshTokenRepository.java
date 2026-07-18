package com.everypicfound.identity.domain.repository;

import com.everypicfound.identity.domain.model.token.RefreshTokenSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;

import java.time.Instant;
import java.util.Optional;

/**
 * Refresh Token 仓储。
 */
public interface UserRefreshTokenRepository {

    void save(UserRefreshToken refreshToken);

    Optional<RefreshTokenSession> findSessionByTokenHash(String tokenHash);

    boolean markActiveTokenAsUsed(String tokenHash, Instant usedAt);

    int revokeActiveTokensBySessionId(String sessionId, Instant revokedAt);

    int revokeActiveTokensByUserId(long userId, Instant revokedAt);
}
