package com.everypicfound.identity.infrastructure.config.properties;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 密码哈希配置。
 */
@Validated
@ConfigurationProperties(prefix = "everypicfound.auth.password")
public record PasswordHashProperties(
        @Min(4) @Max(31) int bcryptStrength) {
}
