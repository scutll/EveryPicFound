package com.everypicfound.common.cache;
public class CacheKeyBuilder {

    // 提供缓存统一接口，MVP 空实现，后续接 Redis。
    private CacheService cacheService;

    // 统一生成搜索缓存 key。
    public String buildSearchKey(String rawKey) {
        throw new UnsupportedOperationException("TODO");
    }

    // 统一生成向量缓存 key。
    public String buildVectorKey(String rawKey) {
        throw new UnsupportedOperationException("TODO");
    }

    // 统一生成图片缓存 key。
    public String buildImageKey(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }
}
