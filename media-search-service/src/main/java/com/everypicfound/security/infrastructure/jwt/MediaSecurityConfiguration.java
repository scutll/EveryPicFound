package com.everypicfound.security.infrastructure.jwt;

import com.everypicfound.security.contract.SecurityScopes;
import com.everypicfound.security.contract.jwt.JwtAudienceValidator;
import com.everypicfound.security.contract.jwt.JwtRequiredClaimsValidator;
import com.everypicfound.security.contract.jwt.PemRsaPublicKeyLoader;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableConfigurationProperties(MediaJwtProperties.class)
public class MediaSecurityConfiguration {

    @Bean
    SecurityFilterChain mediaSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/images/upload")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_UPLOAD)
                        .requestMatchers("/api/search/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_SEARCH)
                        .requestMatchers("/api/images/**", "/images/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_READ)
                        .requestMatchers("/internal/images/**")
                        .authenticated()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {
                }))
                .build();
    }

    @Bean
    JwtDecoder mediaJwtDecoder(MediaJwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(new PemRsaPublicKeyLoader().load(properties.publicKeyLocation()))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(properties.clockSkew());
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(List.of(
                timestampValidator,
                new JwtIssuerValidator(properties.issuer()),
                new JwtAudienceValidator(properties.audience()),
                new JwtRequiredClaimsValidator())));
        return decoder;
    }

    @Bean
    Clock mediaClock() {
        return Clock.systemUTC();
    }
}
