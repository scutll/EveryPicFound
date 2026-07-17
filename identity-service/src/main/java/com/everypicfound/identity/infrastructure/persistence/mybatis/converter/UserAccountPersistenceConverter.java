package com.everypicfound.identity.infrastructure.persistence.mybatis.converter;

import com.everypicfound.identity.domain.enums.AccountStatus;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.UserAuthentication;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 用户领域模型到 MyBatis 持久化对象的边界转换器。
 */
@Component
public final class UserAccountPersistenceConverter {

    public UserAccountPo toPo(UserAccount account) {
        Objects.requireNonNull(account, "account");

        UserAccountPo po = new UserAccountPo();
        po.setId(account.id());
        po.setUsername(account.username().value());
        po.setPasswordHash(account.passwordHash().value());
        po.setNickname(account.nickname().map(nickname -> nickname.value())
                .orElse(null));
        po.setAvatarUrl(account.avatarUrl());
        po.setStatus(account.status().name());
        po.setAuthValidAfter(toUtcDateTime(account.authValidAfter()));
        po.setLastLoginTime(toUtcDateTime(account.lastLoginTime()));
        po.setVersion(account.version());
        po.setCreatedTime(toUtcDateTime(account.createdTime()));
        po.setUpdatedTime(toUtcDateTime(account.updatedTime()));
        return po;
    }

    public UserAuthentication toAuthentication(UserAccountPo po) {
        Objects.requireNonNull(po, "po");
        return new UserAuthentication(
                po.getId(),
                PasswordHash.of(po.getPasswordHash()),
                AccountStatus.valueOf(po.getStatus()),
                toInstant(po.getAuthValidAfter()));
    }

    private static LocalDateTime toUtcDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
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
