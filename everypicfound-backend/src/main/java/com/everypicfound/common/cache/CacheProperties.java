package com.everypicfound.common.cache;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "everypicfound.cache")
public class CacheProperties {

    // 配置开关。
    private Boolean enabled;

    // 缓存 TTL。
    private Long ttlSeconds;

    // 缓存 key 前缀。
    private String keyPrefix;
}
