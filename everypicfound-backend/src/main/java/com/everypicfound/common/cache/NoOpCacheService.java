package com.everypicfound.common.cache;

import org.springframework.stereotype.Component;


@Component
public class NoOpCacheService implements CacheService {

    @Override
    // 获取当前请求或任务上下文。
    public Object get(String key) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // MVP 空实现，不写缓存。
    public void put(String key, Object value) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // MVP 空实现，不删除缓存。
    public void evict(String key) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // MVP 空实现，默认不存在。
    public boolean exists(String key) {
        throw new UnsupportedOperationException("TODO");
    }
}
