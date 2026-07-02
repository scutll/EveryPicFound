package com.everypicfound.common.cache;

import lombok.Data;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.constraints.NotBlank;
@Data
@ConfigurationProperties(prefix = "everypicfound.cache")
public class CacheProperties {

    // 配置开关。
    private Boolean enabled = false;

    // 缓存 TTL。
    private Long ttlSeconds = 300L;

    // 缓存 key 前缀。
    @NotBlank
    private String keyPrefix = "epf:dev";

    public Duration getDefaultTtl() {
        return Duration.ofSeconds(ttlSeconds);
    }
}
