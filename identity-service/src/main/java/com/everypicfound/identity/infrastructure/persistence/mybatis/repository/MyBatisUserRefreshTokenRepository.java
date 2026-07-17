package com.everypicfound.identity.infrastructure.persistence.mybatis.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.everypicfound.identity.domain.model.session.UserSessionStatus;
import com.everypicfound.identity.domain.model.token.RefreshTokenSession;
import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.domain.model.token.UserRefreshTokenStatus;
import com.everypicfound.identity.domain.repository.UserRefreshTokenRepository;
import com.everypicfound.identity.infrastructure.persistence.mybatis.converter.UserRefreshTokenPersistenceConverter;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserRefreshTokenMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.mapper.UserSessionMapper;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * 基于 MyBatis-Plus 的 Refresh Token 仓储适配器。
 */
@Repository
public class MyBatisUserRefreshTokenRepository
        implements UserRefreshTokenRepository {

    private final UserRefreshTokenMapper mapper;
    private final UserSessionMapper sessionMapper;
    private final UserRefreshTokenPersistenceConverter converter;

    public MyBatisUserRefreshTokenRepository(
            UserRefreshTokenMapper mapper,
            UserSessionMapper sessionMapper,
            UserRefreshTokenPersistenceConverter converter) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.sessionMapper = Objects.requireNonNull(
                sessionMapper,
                "sessionMapper");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    @Override
    public void save(UserRefreshToken refreshToken) {
        UserRefreshTokenPo po = converter.toPo(refreshToken);
        int affectedRows = mapper.insert(po);
        if (affectedRows != 1) {
            throw new IllegalStateException(
                    "expected one inserted refresh token row");
        }
    }

    @Override
    public Optional<RefreshTokenSession> findSessionByTokenHash(
            String tokenHash) {
        Objects.requireNonNull(tokenHash, "tokenHash");
        UserRefreshTokenPo refreshToken = mapper.selectById(tokenHash);
        if (refreshToken == null) {
            return Optional.empty();
        }
        UserSessionPo session = sessionMapper.selectById(
                refreshToken.getSessionId());
        if (session == null) {
            return Optional.empty();
        }
        return Optional.of(new RefreshTokenSession(
                refreshToken.getTokenHash(),
                UserRefreshTokenStatus.valueOf(refreshToken.getStatus()),
                toInstant(refreshToken.getExpiresTime()),
                session.getSessionId(),
                session.getUserId(),
                UserSessionStatus.valueOf(session.getStatus()),
                toInstant(session.getCreatedTime()),
                toInstant(session.getExpiresTime())));
    }

    @Override
    public boolean markActiveTokenAsUsed(String tokenHash, Instant usedAt) {
        Objects.requireNonNull(tokenHash, "tokenHash");
        Objects.requireNonNull(usedAt, "usedAt");
        int affectedRows = mapper.update(
                null,
                Wrappers.<UserRefreshTokenPo>update()
                        .set("status", UserRefreshTokenStatus.USED.name())
                        .set("used_time", toUtcDateTime(usedAt))
                        .eq("token_hash", tokenHash)
                        .eq("status", UserRefreshTokenStatus.ACTIVE.name()));
        return affectedRows == 1;
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        Instant millisecondPrecision = instant.truncatedTo(ChronoUnit.MILLIS);
        return LocalDateTime.ofInstant(millisecondPrecision, ZoneOffset.UTC);
    }

    private static Instant toInstant(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toInstant(ZoneOffset.UTC);
    }
}
