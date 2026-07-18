package com.everypicfound.identity.domain.repository;

import com.everypicfound.identity.domain.model.session.UserSession;

import java.time.Instant;

/**
 * 用户会话仓储。
 */
public interface UserSessionRepository {

    void save(UserSession session);

    boolean revokeActiveSession(String sessionId, long userId, Instant revokedAt);
}
