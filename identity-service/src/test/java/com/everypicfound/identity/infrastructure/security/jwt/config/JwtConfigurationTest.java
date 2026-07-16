package com.everypicfound.identity.infrastructure.security.jwt.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everypicfound.identity.infrastructure.config.properties.JwtProperties;
import com.everypicfound.identity.infrastructure.security.jwt.key.RsaKeyMaterial;
import com.everypicfound.identity.support.security.TestRsaKeyMaterial;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtConfigurationTest {

    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");

    @TempDir
    Path tempDirectory;

    private JwtConfiguration configuration;
    private JwtProperties properties;
    private RsaKeyMaterial keyMaterial;
    private JwtEncoder encoder;
    private JwtDecoder decoder;

    @BeforeEach
    void setUp() {
        var keyPair = TestRsaKeyMaterial.generate(2048);
        Path privateKey = TestRsaKeyMaterial.writePrivateKey(tempDirectory, "private.pem", keyPair);
        Path publicKey = TestRsaKeyMaterial.writePublicKey(tempDirectory, "public.pem", keyPair);
        properties = new JwtProperties(
                "everypicfound-identity",
                "everypicfound-api",
                Duration.ofMinutes(30),
                Duration.ofSeconds(30),
                new FileSystemResource(privateKey),
                new FileSystemResource(publicKey));
        configuration = new JwtConfiguration();
        keyMaterial = configuration.rsaKeyMaterial(properties);
        encoder = configuration.jwtEncoder(keyMaterial);
        decoder = configuration.jwtDecoder(
                keyMaterial, properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void configuresRs256RoundTripWithRequiredValidators() {
        String token = encode("everypicfound-identity", List.of("everypicfound-api"));

        var decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getHeaders()).containsEntry("alg", "RS256");
    }

    @Test
    void decoderRejectsUnexpectedIssuerAndAudience() {
        assertThatThrownBy(() -> decoder.decode(encode("other-issuer", List.of("everypicfound-api"))))
                .isInstanceOf(JwtValidationException.class);
        assertThatThrownBy(() -> decoder.decode(encode("everypicfound-identity", List.of("other-api"))))
                .isInstanceOf(JwtValidationException.class);
    }

    @Test
    void decoderRejectsTamperingAndWrongPublicKey() {
        String token = encode("everypicfound-identity", List.of("everypicfound-api"));
        String[] segments = token.split("\\.");
        char replacement = segments[2].charAt(0) == 'A' ? 'B' : 'A';
        segments[2] = replacement + segments[2].substring(1);
        String tampered = String.join(".", segments);

        assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);

        var otherKeyPair = TestRsaKeyMaterial.generate(2048);
        JwtDecoder wrongKeyDecoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withPublicKey((java.security.interfaces.RSAPublicKey) otherKeyPair.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        assertThatThrownBy(() -> wrongKeyDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void decoderAppliesThirtySecondClockSkewToExpiryAndNotBefore() {
        assertThat(decoder.decode(encodeWithTimes(
                        NOW.minusSeconds(1800), NOW.minusSeconds(1800), NOW.minusSeconds(29))))
                .isNotNull();
        assertThatThrownBy(() -> decoder.decode(encodeWithTimes(
                        NOW.minusSeconds(1800), NOW.minusSeconds(1800), NOW.minusSeconds(31))))
                .isInstanceOf(JwtValidationException.class);

        assertThat(decoder.decode(encodeWithTimes(NOW, NOW.plusSeconds(29), NOW.plusSeconds(1800))))
                .isNotNull();
        assertThatThrownBy(() -> decoder.decode(encodeWithTimes(
                        NOW, NOW.plusSeconds(31), NOW.plusSeconds(1800))))
                .isInstanceOf(JwtValidationException.class);
    }

    private String encode(String issuer, List<String> audience) {
        return encode(issuer, audience, NOW, NOW, NOW.plusSeconds(1800));
    }

    private String encodeWithTimes(Instant issuedAt, Instant notBefore, Instant expiresAt) {
        return encode("everypicfound-identity", List.of("everypicfound-api"), issuedAt, notBefore, expiresAt);
    }

    private String encode(
            String issuer,
            List<String> audience,
            Instant issuedAt,
            Instant notBefore,
            Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("42")
                .audience(audience)
                .issuedAt(issuedAt)
                .notBefore(notBefore)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("sid", "session-1")
                .claim("scope", "image:read image:write")
                .claim("auth_time", NOW.getEpochSecond())
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
