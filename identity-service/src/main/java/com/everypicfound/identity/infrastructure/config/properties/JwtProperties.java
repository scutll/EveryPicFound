package com.everypicfound.identity.infrastructure.config.properties;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Access Token 的签发与验证配置。
 */
@ConfigurationProperties(prefix = "everypicfound.auth.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration clockSkew,
        Resource privateKeyLocation,
        Resource publicKeyLocation) {

    public JwtProperties {
        requireText(issuer, "issuer");
        requireText(audience, "audience");
        Objects.requireNonNull(accessTokenTtl, "accessTokenTtl must not be null");
        Objects.requireNonNull(clockSkew, "clockSkew must not be null");
        Objects.requireNonNull(privateKeyLocation, "privateKeyLocation must not be null");
        Objects.requireNonNull(publicKeyLocation, "publicKeyLocation must not be null");
        if (accessTokenTtl.isZero() || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException("accessTokenTtl must be positive");
        }
        if (clockSkew.isNegative()) {
            throw new IllegalArgumentException("clockSkew must not be negative");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
