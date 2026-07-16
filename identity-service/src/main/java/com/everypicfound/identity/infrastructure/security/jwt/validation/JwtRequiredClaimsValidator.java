package com.everypicfound.identity.infrastructure.security.jwt.validation;

import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtRequiredClaimsValidator implements OAuth2TokenValidator<Jwt> {

    static final List<String> REQUIRED_CLAIMS = List.of(
            "iss", "sub", "aud", "iat", "nbf", "exp", "jti", "sid", "scope", "auth_time");

    private static final List<String> REQUIRED_TEXT_CLAIMS = List.of("iss", "sub", "jti", "sid", "scope");
    private static final OAuth2Error INVALID_CLAIMS = new OAuth2Error(
            "invalid_token", "The token is missing one or more required claims", null);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        Map<String, Object> claims = token.getClaims();
        boolean missingClaim = REQUIRED_CLAIMS.stream()
                .anyMatch(name -> !claims.containsKey(name) || claims.get(name) == null);
        boolean blankTextClaim = REQUIRED_TEXT_CLAIMS.stream()
                .anyMatch(name -> !(claims.get(name) instanceof String value) || value.isBlank());
        boolean emptyAudience = token.getAudience() == null || token.getAudience().isEmpty();
        if (missingClaim || blankTextClaim || emptyAudience) {
            return OAuth2TokenValidatorResult.failure(INVALID_CLAIMS);
        }
        return OAuth2TokenValidatorResult.success();
    }
}
