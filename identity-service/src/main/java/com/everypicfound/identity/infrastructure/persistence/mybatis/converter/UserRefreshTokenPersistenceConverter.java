package com.everypicfound.identity.infrastructure.persistence.mybatis.converter;

import com.everypicfound.identity.domain.model.token.UserRefreshToken;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserRefreshTokenPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Refresh Token 领域模型到 MyBatis 持久化对象的边界转换器。
 */
@Component
public final class UserRefreshTokenPersistenceConverter {

    public UserRefreshTokenPo toPo(UserRefreshToken refreshToken) {
        Objects.requireNonNull(refreshToken, "refreshToken");

        UserRefreshTokenPo po = new UserRefreshTokenPo();
        po.setSessionId(refreshToken.sessionId());
        po.setTokenHash(refreshToken.tokenHash());
        po.setStatus(refreshToken.status().name());
        po.setIssuedTime(toUtcDateTime(refreshToken.issuedAt()));
        po.setExpiresTime(toUtcDateTime(refreshToken.expiresAt()));
        return po;
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        Instant millisecondPrecision = instant.truncatedTo(ChronoUnit.MILLIS);
        return LocalDateTime.ofInstant(millisecondPrecision, ZoneOffset.UTC);
    }
}
