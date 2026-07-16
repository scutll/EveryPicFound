package com.everypicfound.identity.infrastructure.security.jwt.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtAudienceValidatorTest {

    private final JwtAudienceValidator validator = new JwtAudienceValidator("everypicfound-api");

    @Test
    void acceptsExpectedAudienceAmongMultipleAudiences() {
        assertThat(validator.validate(jwtWithAudience(List.of("another-api", "everypicfound-api"))).hasErrors())
                .isFalse();
    }

    @Test
    void rejectsMissingOrUnexpectedAudience() {
        assertThat(validator.validate(jwtWithAudience(List.of("another-api"))).hasErrors()).isTrue();
        assertThat(validator.validate(jwtWithAudience(null)).hasErrors()).isTrue();
    }

    private static Jwt jwtWithAudience(List<String> audience) {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800));
        if (audience != null) {
            builder.claim("aud", audience);
        }
        return builder.build();
    }
}
