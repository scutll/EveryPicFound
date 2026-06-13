package com.everypicfound.common.cache;

import java.time.Duration;

import org.springframework.stereotype.Component;


@Component
public class NoOpCacheService implements CacheService {

    @Override
    public <T> T get(String key, Class<T> valueType) {
        return null;
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        //no op
    }

    @Override
    public void evict(String key) {
        //no op
    }

    @Override
    public boolean exists(String key) {
        return false;
    }
}
