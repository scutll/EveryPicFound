package com.everypicfound.identity.infrastructure.security.jwt.adapter;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.exception.InvalidAccessTokenIssueRequestException;
import com.everypicfound.identity.application.port.out.AccessTokenIssuer;
import com.everypicfound.identity.application.result.IssuedAccessToken;
import com.everypicfound.identity.infrastructure.config.properties.JwtProperties;
import com.everypicfound.identity.support.exception.AccessTokenIssuanceException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.stereotype.Component;

/**
 * 使用 Spring Security JOSE 和 RSA 私钥签发 Access Token。
 */
@Component
public final class SpringJoseAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public SpringJoseAccessTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock clock) {
        this.encoder = Objects.requireNonNull(encoder, "encoder must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public IssuedAccessToken issue(AccessTokenIssueRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Instant issuedAt = Instant.ofEpochSecond(clock.instant().getEpochSecond());
        if (request.authTime().isAfter(issuedAt)) {
            throw new InvalidAccessTokenIssueRequestException(
                    "authTime must not be later than issuedAt");
        }
        Instant expiresAt = issuedAt.plus(properties.accessTokenTtl());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(Long.toString(request.userId()))
                .audience(java.util.List.of(properties.audience()))
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", request.sessionId())
                .claim("scope", String.join(" ", request.scopes()))
                .claim("auth_time", request.authTime().getEpochSecond())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();
        try {
            String tokenValue = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
            return new IssuedAccessToken(tokenValue, expiresAt);
        } catch (JwtEncodingException exception) {
            throw new AccessTokenIssuanceException("Access Token signing failed", exception);
        }
    }
}
