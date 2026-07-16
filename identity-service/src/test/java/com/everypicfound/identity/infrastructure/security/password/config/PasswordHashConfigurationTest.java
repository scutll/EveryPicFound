package com.everypicfound.identity.infrastructure.security.password.config;

import com.everypicfound.identity.infrastructure.config.properties.PasswordHashProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordHashConfigurationTest {

    @Test
    void createsDelegatingEncoderWithConfiguredBcryptStrength() {
        PasswordHashConfiguration configuration =
                new PasswordHashConfiguration();

        PasswordEncoder encoder = configuration.passwordEncoder(
                new PasswordHashProperties(4));

        assertThat(encoder.encode("secret123"))
                .startsWith("{bcrypt}$2a$04$");
    }
}
