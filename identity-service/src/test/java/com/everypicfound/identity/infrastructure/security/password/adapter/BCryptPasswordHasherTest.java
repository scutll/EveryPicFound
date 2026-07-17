package com.everypicfound.identity.infrastructure.security.password.adapter;

import com.everypicfound.identity.domain.model.user.PasswordHash;
import com.everypicfound.identity.domain.model.user.PresentedPassword;
import com.everypicfound.identity.domain.model.user.RawPassword;
import com.everypicfound.identity.support.exception.PasswordHashingException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BCryptPasswordHasherTest {

    @Test
    void hashesWithBcryptPrefixAndMatchesTheOriginalPassword() {
        BCryptPasswordHasher hasher =
                new BCryptPasswordHasher(testPasswordEncoder());
        RawPassword rawPassword = RawPassword.of("secret123");

        PasswordHash passwordHash = hasher.hash(rawPassword);

        assertThat(passwordHash.value()).startsWith("{bcrypt}$2a$04$");
        assertThat(hasher.matches(
                PresentedPassword.of("secret123"),
                passwordHash)).isTrue();
        assertThat(hasher.matches(
                PresentedPassword.of("different123"),
                passwordHash)).isFalse();
    }

    @Test
    void usesANewRandomSaltForEachHash() {
        BCryptPasswordHasher hasher =
                new BCryptPasswordHasher(testPasswordEncoder());
        RawPassword rawPassword = RawPassword.of("secret123");

        PasswordHash first = hasher.hash(rawPassword);
        PasswordHash second = hasher.hash(rawPassword);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void wrapsPasswordEncoderFailuresWithoutExposingThePassword() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode("secret123"))
                .thenThrow(new IllegalStateException("encoder failed"));
        BCryptPasswordHasher hasher = new BCryptPasswordHasher(encoder);

        assertThatThrownBy(() -> hasher.hash(
                RawPassword.of("secret123")))
                .isInstanceOf(PasswordHashingException.class)
                .hasMessageNotContaining("secret123")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private static PasswordEncoder testPasswordEncoder() {
        return new DelegatingPasswordEncoder(
                "bcrypt",
                Map.of("bcrypt", new BCryptPasswordEncoder(4)));
    }
}
