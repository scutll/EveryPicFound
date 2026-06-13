package com.everypicfound.vectorization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.vectorization.cache")
public class VectorCacheProperties {

    // 文本向量缓存开关。
    private Boolean enabled = false;

    // 文本向量缓存 TTL，单位：秒。
    private Long textVectorTtlSeconds = 3600L;
}