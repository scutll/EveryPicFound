package com.everypicfound.interaction.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticatedUserIdResolverTest {

    private final AuthenticatedUserIdResolver resolver =
            new AuthenticatedUserIdResolver();

    @Test
    void resolvesPositiveLongSubject() {
        assertThat(resolver.resolve(jwtWithSubject("42")))
                .isEqualTo(42L);
    }

    @Test
    void rejectsZeroSubject() {
        assertThatThrownBy(() -> resolver.resolve(
                jwtWithSubject("0")))
                .isInstanceOf(
                        InvalidAuthenticatedUserException.class);
    }

    @Test
    void rejectsNonNumericSubject() {
        assertThatThrownBy(() -> resolver.resolve(
                jwtWithSubject("user-42")))
                .isInstanceOf(
                        InvalidAuthenticatedUserException.class);
    }

    private Jwt jwtWithSubject(String subject) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .build();
    }
}
