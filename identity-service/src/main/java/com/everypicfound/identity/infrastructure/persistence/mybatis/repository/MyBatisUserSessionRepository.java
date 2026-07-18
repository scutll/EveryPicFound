package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.everypicfound.identity.domain.model.session.UserSession;
import com.everypicfound.identity.domain.model.session.UserSessionStatus;
import com.everypicfound.identity.domain.repository.UserSessionRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserSessionPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserSessionMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 基于 MyBatis-Plus 的用户会话仓储适配器。
 */
@Repository
public class MyBatisUserSessionRepository implements UserSessionRepository {

    private final UserSessionMapper mapper;
    private final UserSessionPersistenceConverter converter;

    public MyBatisUserSessionRepository(
            UserSessionMapper mapper,
            UserSessionPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public void save(UserSession session) {
        UserSessionPo po = converter.toPo(session);
        int affectedRows = mapper.insert(po);
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "expected one inserted user session row");
        }
    }

    @Override
    public boolean revokeActiveSession(
            String sessionId,
            long userId,
            Instant revokedAt) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(revokedAt, "revokedAt");
        int affectedRows = mapper.update(
                null,
                Wrappers.<UserSessionPo>update()
                        .set("status", UserSessionStatus.REVOKED.name())
                        .set("revoked_time", toUtcDateTime(revokedAt))
                        .eq("session_id", sessionId)
                        .eq("user_id", userId)
                        .eq("status", UserSessionStatus.ACTIVE.name()));
        return affectedRows == 1;
    }

    @Override
    public int revokeActiveSessionsByUserId(long userId, Instant revokedAt) {
        Objects.requireNonNull(revokedAt, "revokedAt");
        return mapper.update(
                null,
                Wrappers.<UserSessionPo>update()
                        .set("status", UserSessionStatus.REVOKED.name())
                        .set("revoked_time", toUtcDateTime(revokedAt))
                        .eq("user_id", userId)
                        .eq("status", UserSessionStatus.ACTIVE.name()));
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        Instant millisecondPrecision = instant.truncatedTo(ChronoUnit.MILLIS);
        return LocalDateTime.ofInstant(millisecondPrecision, ZoneOffset.UTC);
    }
}
