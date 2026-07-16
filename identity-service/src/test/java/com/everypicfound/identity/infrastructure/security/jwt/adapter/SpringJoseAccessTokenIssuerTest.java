package com.everypicfound.identity.infrastructure.security.jwt.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.exception.InvalidAccessTokenIssueRequestException;
import com.everypicfound.identity.infrastructure.config.properties.JwtProperties;
import com.everypicfound.identity.support.exception.AccessTokenIssuanceException;
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
import org.springframework.security.oauth2.jwt.JwtEncodingException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

class SpringJoseAccessTokenIssuerTest {

    private static final Instant NOW = Instant.parse("2026-07-16T12:00:00Z");
    private static final Instant AUTH_TIME = Instant.parse("2026-07-16T11:59:00Z");

    @TempDir
    Path tempDirectory;

    private JwtProperties properties;
    private NimbusJwtEncoder encoder;
    private NimbusJwtDecoder decoder;

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

        RSAKey rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .build();
        encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey)));
        decoder = NimbusJwtDecoder.withPublicKey((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(token -> org.springframework.security.oauth2.core.OAuth2TokenValidatorResult.success());
    }

    @Test
    void issuesSignedRs256AccessTokenWithCanonicalClaims() {
        var issuer = new SpringJoseAccessTokenIssuer(
                encoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new AccessTokenIssueRequest(
                42L,
                "session-1",
                List.of("image:write", "image:read", "image:read"),
                AUTH_TIME);

        var issued = issuer.issue(request);
        var decoded = decoder.decode(issued.tokenValue());

        assertThat(decoded.getHeaders())
                .containsEntry("alg", "RS256")
                .containsEntry("typ", "JWT")
                .doesNotContainKey("kid");
        assertThat(decoded.getClaimAsString("iss")).isEqualTo("everypicfound-identity");
        assertThat(decoded.getAudience()).containsExactly("everypicfound-api");
        assertThat(decoded.getSubject()).isEqualTo("42");
        assertThat(decoded.getIssuedAt()).isEqualTo(NOW);
        assertThat(decoded.getNotBefore()).isEqualTo(NOW);
        assertThat(decoded.getExpiresAt()).isEqualTo(NOW.plusSeconds(1800));
        assertThat(decoded.getId()).satisfies(value -> UUID.fromString(value));
        assertThat(decoded.getClaimAsString("sid")).isEqualTo("session-1");
        assertThat(decoded.getClaimAsString("scope")).isEqualTo("image:read image:write");
        assertThat(((Number) decoded.getClaim("auth_time")).longValue()).isEqualTo(AUTH_TIME.getEpochSecond());
        assertThat(issued.expiresAt()).isEqualTo(NOW.plusSeconds(1800));
    }

    @Test
    void generatesANewJtiForEachIssuedToken() {
        var issuer = new SpringJoseAccessTokenIssuer(
                encoder, properties, Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new AccessTokenIssueRequest(42L, "session-1", List.of("image:read"), AUTH_TIME);

        String firstJti = decoder.decode(issuer.issue(request).tokenValue()).getId();
        String secondJti = decoder.decode(issuer.issue(request).tokenValue()).getId();

        assertThat(firstJti).isNotEqualTo(secondJti);
    }

    @Test
    void translatesEncoderFailureWithoutExposingRequestOrTokenMaterial() {
        JwtEncodingException cause = new JwtEncodingException("synthetic encoder failure");
        var issuer = new SpringJoseAccessTokenIssuer(
                parameters -> {
                    throw cause;
                },
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new AccessTokenIssueRequest(42L, "sensitive-session", List.of("image:read"), AUTH_TIME);

        assertThatThrownBy(() -> issuer.issue(request))
                .isInstanceOf(AccessTokenIssuanceException.class)
                .hasMessage("Access Token signing failed")
                .hasCause(cause)
                .hasMessageNotContaining("sensitive-session");
    }

    @Test
    void rejectsAuthenticationTimeLaterThanIssueTimeBeforeEncoding() {
        var issuer = new SpringJoseAccessTokenIssuer(
                parameters -> {
                    throw new AssertionError("encoder must not be called");
                },
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
        var request = new AccessTokenIssueRequest(
                42L, "session-1", List.of("image:read"), NOW.plusSeconds(1));

        assertThatThrownBy(() -> issuer.issue(request))
                .isInstanceOf(InvalidAccessTokenIssueRequestException.class)
                .hasMessageContaining("authTime");
    }
}
