package com.everypicfound.common.cache;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CacheKeyBuilder {


    private static final String DEFAULT_KEY_PREFIX = "epf:dev";

    private final CacheProperties cacheProperties;

    // 构建缓存 key 的通用方法，按照模块、业务和细节进行分层组织。
    private String buildKey(String module, String biz, String detail) {
        return String.join(":", resolveKeyPrefix(), module, biz, detail);
    }

    // 解析缓存 key 前缀，如果配置项为空，则返回默认值。
    private String resolveKeyPrefix(){
        String configuredPrefix = cacheProperties.getKeyPrefix();
        if(!StringUtils.hasText(configuredPrefix)) {
            return DEFAULT_KEY_PREFIX;
        }
        return StringUtils.trimTrailingCharacter(configuredPrefix.trim(), ':');// 移除末尾冒号，避免重复分隔符
    }

    // 统一生成搜索结果缓存 key：epf:{env}:search:result:{detail}。
    public String buildSearchKey(String rawKey) {
        return buildKey("search","result", rawKey);
    }

    // 统一生成文本向量缓存 key：epf:{env}:vectorization:text-vector:{detail}。
    public String buildVectorKey(String rawKey) {
        return buildKey("vectorization","text-vector", rawKey);
    }

    // 统一生成图片资源缓存 key：epf:{env}:imageasset:asset:{imageId}。
    public String buildImageKey(Long imageId) {
        return buildKey("imageasset", "asset", String.valueOf(imageId));// 将 imageId 转为字符串作为细节部分
    }
}
