package com.everypicfound.interaction.infrastructure.security.jwt;

import com.everypicfound.interaction.support.web.InteractionSecurityErrorWriter;
import com.everypicfound.security.contract.jwt.JwtAudienceValidator;
import com.everypicfound.security.contract.jwt.JwtRequiredClaimsValidator;
import com.everypicfound.security.contract.jwt.PemRsaPublicKeyLoader;
import java.time.Clock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
@EnableConfigurationProperties(InteractionJwtProperties.class)
public class InteractionSecurityConfiguration {

    @Bean
    SecurityFilterChain interactionSecurityFilterChain(
            HttpSecurity http,
            InteractionSecurityErrorWriter errorWriter) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                errorWriter::writeUnauthorized)
                        .accessDeniedHandler(
                                errorWriter::writeForbidden))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(
                                errorWriter::writeUnauthorized)
                        .accessDeniedHandler(
                                errorWriter::writeForbidden)
                        .jwt(jwt -> {
                        }))
                .build();
    }

    @Bean
    JwtDecoder interactionJwtDecoder(
            InteractionJwtProperties properties,
            Clock interactionClock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withPublicKey(new PemRsaPublicKeyLoader()
                        .load(properties.publicKeyLocation()))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        JwtTimestampValidator timestampValidator =
                new JwtTimestampValidator(properties.clockSkew());
        timestampValidator.setClock(interactionClock);
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(List.of(
                        timestampValidator,
                        new JwtIssuerValidator(properties.issuer()),
                        new JwtAudienceValidator(properties.audience()),
                        new JwtRequiredClaimsValidator())));
        return decoder;
    }

    @Bean
    Clock interactionClock() {
        return Clock.systemUTC();
    }
}
