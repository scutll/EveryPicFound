package com.everypicfound.identity.infrastructure.config.properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class JwtPropertiesTest {

    private static final ByteArrayResource KEY_RESOURCE = new ByteArrayResource(new byte[] {1});

    @Test
    void acceptsValidConfiguration() {
        new JwtProperties(
                "everypicfound-identity",
                "everypicfound-api",
                Duration.ofMinutes(30),
                Duration.ofSeconds(30),
                KEY_RESOURCE,
                KEY_RESOURCE);
    }

    @Test
    void rejectsBlankIssuerOrAudience() {
        assertThatThrownBy(() -> properties(" ", "everypicfound-api", Duration.ofMinutes(30), Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
        assertThatThrownBy(() -> properties("everypicfound-identity", "\n", Duration.ofMinutes(30), Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("audience");
    }

    @Test
    void rejectsNonPositiveTtlAndNegativeClockSkew() {
        assertThatThrownBy(() -> properties("everypicfound-identity", "everypicfound-api", Duration.ZERO, Duration.ofSeconds(30)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("accessTokenTtl");
        assertThatThrownBy(() -> properties("everypicfound-identity", "everypicfound-api", Duration.ofMinutes(30), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clockSkew");
    }

    @Test
    void rejectsMissingKeyLocations() {
        assertThatThrownBy(() -> new JwtProperties(
                "everypicfound-identity", "everypicfound-api", Duration.ofMinutes(30), Duration.ofSeconds(30), null, KEY_RESOURCE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("privateKeyLocation");
        assertThatThrownBy(() -> new JwtProperties(
                "everypicfound-identity", "everypicfound-api", Duration.ofMinutes(30), Duration.ofSeconds(30), KEY_RESOURCE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("publicKeyLocation");
    }

    private static JwtProperties properties(String issuer, String audience, Duration ttl, Duration skew) {
        return new JwtProperties(issuer, audience, ttl, skew, KEY_RESOURCE, KEY_RESOURCE);
    }
}
