package com.everypicfound.identity.infrastructure.security.jwt.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class JwtRequiredClaimsValidatorTest {

    private final JwtRequiredClaimsValidator validator = new JwtRequiredClaimsValidator();

    @Test
    void acceptsCompleteAccessTokenClaims() {
        assertThat(validator.validate(jwt(validClaims())).hasErrors()).isFalse();
    }

    @Test
    void rejectsEveryMissingRequiredClaim() {
        for (String requiredClaim : JwtRequiredClaimsValidator.REQUIRED_CLAIMS) {
            Map<String, Object> claims = new HashMap<>(validClaims());
            claims.remove(requiredClaim);

            assertThat(validator.validate(jwt(claims)).hasErrors())
                    .as("claim %s must be required", requiredClaim)
                    .isTrue();
        }
    }

    @Test
    void rejectsBlankStringClaimsAndEmptyAudience() {
        for (String stringClaim : List.of("iss", "sub", "jti", "sid", "scope")) {
            Map<String, Object> claims = new HashMap<>(validClaims());
            claims.put(stringClaim, " ");
            assertThat(validator.validate(jwt(claims)).hasErrors())
                    .as("claim %s must contain text", stringClaim)
                    .isTrue();
        }

        Map<String, Object> emptyAudience = new HashMap<>(validClaims());
        emptyAudience.put("aud", List.of());
        assertThat(validator.validate(jwt(emptyAudience)).hasErrors()).isTrue();
    }

    private static Map<String, Object> validClaims() {
        Instant now = Instant.parse("2026-07-16T12:00:00Z");
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", "everypicfound-identity");
        claims.put("sub", "42");
        claims.put("aud", List.of("everypicfound-api"));
        claims.put("iat", now);
        claims.put("nbf", now);
        claims.put("exp", now.plusSeconds(1800));
        claims.put("jti", "550e8400-e29b-41d4-a716-446655440000");
        claims.put("sid", "session-1");
        claims.put("scope", "image:read image:write");
        claims.put("auth_time", now);
        return claims;
    }

    private static Jwt jwt(Map<String, Object> claims) {
        Instant issuedAt = claims.get("iat") instanceof Instant value ? value : null;
        Instant expiresAt = claims.get("exp") instanceof Instant value ? value : null;
        return new Jwt("token", issuedAt, expiresAt, Map.of("alg", "RS256"), claims);
    }
}
