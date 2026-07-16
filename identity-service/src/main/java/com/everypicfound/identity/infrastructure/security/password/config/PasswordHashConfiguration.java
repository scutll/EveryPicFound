package com.everypicfound.identity.infrastructure.security.password.config;

import com.everypicfound.identity.infrastructure.config.properties.PasswordHashProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

/**
 * Spring Security 密码编码器配置。
 */
@Configuration(proxyBeanMethods = false)
public class PasswordHashConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder(
            PasswordHashProperties properties) {
        PasswordEncoder bcrypt = new BCryptPasswordEncoder(
                properties.bcryptStrength());
        return new DelegatingPasswordEncoder(
                "bcrypt",
                Map.of("bcrypt", bcrypt));
    }
}
