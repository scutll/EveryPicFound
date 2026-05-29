package com.everypicfound.common.cache;

import org.springframework.stereotype.Service;

@Service
public class CacheKeyBuilder {


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
