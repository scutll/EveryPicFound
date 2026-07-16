package com.everypicfound.identity.infrastructure.persistence.mybatis.converter;

import com.everypicfound.identity.domain.model.user.Nickname;
import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.UserAccount;
import com.everypicfound.identity.domain.model.user.Username;
import com.everypicfound.identity.infrastructure.persistence.mybatis.po.UserAccountPo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountPersistenceConverterTest {

    @Test
    void convertsNewAccountToPoUsingUtcAndMillisecondPrecision() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-07-16T07:00:00.123456789Z"),
                ZoneOffset.UTC);
        UserAccount account = UserAccount.register(
                Username.of("User_01"),
                PasswordHash.of("{bcrypt}encoded-password"),
                Nickname.optionalOf("探索者"),
                clock);

        UserAccountPo po = new UserAccountPersistenceConverter().toPo(account);

        LocalDateTime expectedTime = LocalDateTime.of(
                2026, 7, 16, 7, 0, 0, 123_000_000);
        assertThat(po.getId()).isNull();
        assertThat(po.getUsername()).isEqualTo("User_01");
        assertThat(po.getPasswordHash()).isEqualTo("{bcrypt}encoded-password");
        assertThat(po.getNickname()).isEqualTo("探索者");
        assertThat(po.getAvatarUrl()).isNull();
        assertThat(po.getStatus()).isEqualTo("NORMAL");
        assertThat(po.getAuthValidAfter()).isEqualTo(expectedTime);
        assertThat(po.getLastLoginTime()).isNull();
        assertThat(po.getVersion()).isZero();
        assertThat(po.getCreatedTime()).isEqualTo(expectedTime);
        assertThat(po.getUpdatedTime()).isEqualTo(expectedTime);
    }
}
