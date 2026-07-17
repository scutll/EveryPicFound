package com.everypicfound.identity.infrastructure.security.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRandomRefreshTokenGeneratorTest {

    @Test
    void generatesUrlSafeOpaqueToken() {
        SecureRandomRefreshTokenGenerator generator =
                new SecureRandomRefreshTokenGenerator();

        String token = generator.generate();

        assertThat(token)
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
    }
}
