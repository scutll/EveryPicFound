package com.everypicfound.identity.domain.model.user;

import com.everypicfound.identity.domain.enums.AccountStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserAccountTest {

    private static final Instant REGISTERED_AT =
            Instant.parse("2026-07-16T07:30:45.123Z");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(REGISTERED_AT, ZoneOffset.UTC);

    @Test
    void shouldInitializeNewAccountFromOneRegistrationInstant() {
        Username username = Username.of("User_01");
        PasswordHash passwordHash =
                PasswordHash.of("{bcrypt}$2a$10$test-hash");
        Optional<Nickname> nickname =
                Nickname.optionalOf("  图友  ");

        UserAccount account = UserAccount.register(
                username,
                passwordHash,
                nickname,
                FIXED_CLOCK);

        assertThat(account.id()).isNull();
        assertThat(account.username()).isEqualTo(username);
        assertThat(account.passwordHash()).isEqualTo(passwordHash);
        assertThat(account.nickname()).contains(nickname.orElseThrow());
        assertThat(account.avatarUrl()).isNull();
        assertThat(account.status()).isEqualTo(AccountStatus.NORMAL);
        assertThat(account.authValidAfter()).isEqualTo(REGISTERED_AT);
        assertThat(account.lastLoginTime()).isNull();
        assertThat(account.version()).isZero();
        assertThat(account.createdTime()).isEqualTo(REGISTERED_AT);
        assertThat(account.updatedTime()).isEqualTo(REGISTERED_AT);
        assertThat(account.displayName()).isEqualTo("图友");
    }

    @Test
    void shouldUseUsernameAsDisplayNameWhenNicknameIsAbsent() {
        UserAccount account = UserAccount.register(
                Username.of("User_01"),
                PasswordHash.of("{bcrypt}$2a$10$test-hash"),
                Optional.empty(),
                FIXED_CLOCK);

        assertThat(account.nickname()).isEmpty();
        assertThat(account.displayName()).isEqualTo("User_01");
    }

    @Test
    void shouldNotExposePasswordHashFromToString() {
        PasswordHash passwordHash =
                PasswordHash.of("{bcrypt}$2a$10$sensitive-hash");

        assertThat(passwordHash.toString())
                .doesNotContain("sensitive-hash");
    }

    @Test
    void shouldRejectBlankPasswordHash() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> PasswordHash.of(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
