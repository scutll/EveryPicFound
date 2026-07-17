package com.everypicfound.gateway.infrastructure.security.jwt;

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
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {

    @Bean
    SecurityWebFilterChain gatewaySecurityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers("/actuator/health", "/actuator/info").permitAll()
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/images/upload")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_UPLOAD)
                        .pathMatchers("/api/search/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_SEARCH)
                        .pathMatchers("/api/images/**", "/images/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.IMAGE_READ)
                        .pathMatchers(HttpMethod.GET, "/api/users/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.USER_READ)
                        .pathMatchers("/api/users/**")
                        .hasAuthority("SCOPE_" + SecurityScopes.USER_WRITE)
                        .anyExchange().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> {
                }))
                .build();
    }

    @Bean
    ReactiveJwtDecoder gatewayJwtDecoder(GatewayJwtProperties properties, Clock clock) {
        NimbusReactiveJwtDecoder decoder = NimbusReactiveJwtDecoder
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
    Clock gatewayClock() {
        return Clock.systemUTC();
    }
}
