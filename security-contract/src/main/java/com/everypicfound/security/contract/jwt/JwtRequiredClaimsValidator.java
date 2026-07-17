package com.everypicfound.security.contract.jwt;

import com.everypicfound.security.contract.SecurityClaimNames;
import java.util.List;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtRequiredClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static final List<String> REQUIRED_CLAIMS = List.of(
            "iss", "sub", "aud", "iat", "nbf", "exp", "jti",
            SecurityClaimNames.SESSION_ID,
            SecurityClaimNames.SCOPE,
            SecurityClaimNames.AUTHENTICATION_TIME);

    private static final List<String> REQUIRED_TEXT_CLAIMS = List.of(
            "iss", "sub", "jti",
            SecurityClaimNames.SESSION_ID,
            SecurityClaimNames.SCOPE);

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        for (String claim : REQUIRED_CLAIMS) {
            if (!token.hasClaim(claim)) {
                return failure("Missing required JWT claim: " + claim);
            }
        }
        for (String claim : REQUIRED_TEXT_CLAIMS) {
            String value = token.getClaimAsString(claim);
            if (value == null || value.isBlank()) {
                return failure("Required JWT claim must not be blank: " + claim);
            }
        }
        if (token.getAudience().isEmpty()) {
            return failure("Required JWT audience must not be empty");
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2TokenValidatorResult failure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null));
    }
}
