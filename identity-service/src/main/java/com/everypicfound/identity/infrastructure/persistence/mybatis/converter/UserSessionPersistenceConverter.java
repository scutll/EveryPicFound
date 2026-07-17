package com.everypicfound.identity.infrastructure.persistence.mybatis.converter;

import com.everypicfound.identity.domain.model.session.UserSession;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserSessionPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 用户会话领域模型到 MyBatis 持久化对象的边界转换器。
 */
@Component
public final class UserSessionPersistenceConverter {

    public UserSessionPo toPo(UserSession session) {
        Objects.requireNonNull(session, "session");

        UserSessionPo po = new UserSessionPo();
        po.setSessionId(session.sessionId());
        po.setUserId(session.userId());
        po.setStatus(session.status().name());
        po.setCreatedTime(toUtcDateTime(session.createdAt()));
        po.setExpiresTime(toUtcDateTime(session.expiresAt()));
        return po;
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        Instant millisecondPrecision = instant.truncatedTo(ChronoUnit.MILLIS);
        return LocalDateTime.ofInstant(millisecondPrecision, ZoneOffset.UTC);
    }
}
