package com.everypicfound.identity.infrastructure.security.jwt.config;

import com.everypicfound.identity.infrastructure.config.properties.JwtProperties;
import com.everypicfound.identity.infrastructure.security.jwt.key.PemRsaKeyLoader;
import com.everypicfound.identity.infrastructure.security.jwt.key.RsaKeyMaterial;
import com.everypicfound.identity.infrastructure.security.jwt.validation.JwtAudienceValidator;
import com.everypicfound.identity.infrastructure.security.jwt.validation.JwtRequiredClaimsValidator;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.time.Clock;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    RsaKeyMaterial rsaKeyMaterial(JwtProperties properties) {
        return new PemRsaKeyLoader().load(
                properties.privateKeyLocation(), properties.publicKeyLocation());
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyMaterial keyMaterial) {
        RSAKey rsaKey = new RSAKey.Builder(keyMaterial.publicKey())
                .privateKey(keyMaterial.privateKey())
                .build();
        ImmutableJWKSet<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JwtDecoder jwtDecoder(RsaKeyMaterial keyMaterial, JwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(keyMaterial.publicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(properties.clockSkew());
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(List.of(
                timestampValidator,
                new JwtIssuerValidator(properties.issuer()),
                new JwtAudienceValidator(properties.audience()),
                new JwtRequiredClaimsValidator())));
        return decoder;
    }
}
