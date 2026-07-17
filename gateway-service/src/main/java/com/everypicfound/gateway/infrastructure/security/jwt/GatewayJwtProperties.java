package com.everypicfound.gateway.infrastructure.security.jwt;

import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

@ConfigurationProperties(prefix = "everypicfound.auth.jwt")
public record GatewayJwtProperties(
        String issuer,
        String audience,
        Duration clockSkew,
        Resource publicKeyLocation) {

    public GatewayJwtProperties {
        requireText(issuer, "issuer");
        requireText(audience, "audience");
        Objects.requireNonNull(clockSkew, "clockSkew must not be null");
        Objects.requireNonNull(publicKeyLocation, "publicKeyLocation must not be null");
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
