package com.everypicfound.identity.infrastructure.security.jwt.validation;

import java.util.Objects;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtAudienceValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error INVALID_AUDIENCE = new OAuth2Error(
            "invalid_token", "The token audience is not accepted", null);

    private final String expectedAudience;

    public JwtAudienceValidator(String expectedAudience) {
        Objects.requireNonNull(expectedAudience, "expectedAudience must not be null");
        if (expectedAudience.isBlank()) {
            throw new IllegalArgumentException("expectedAudience must not be blank");
        }
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        if (token.getAudience() != null && token.getAudience().contains(expectedAudience)) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(INVALID_AUDIENCE);
    }
}
