package com.everypicfound.common.cache;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(
    prefix = "everypicfound.cache",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true
)
public class NoOpCacheService implements CacheService {

    @Override
    // 获取当前请求或任务上下文。
    public <T> T get(String key, Class<T> valueType) {
        return null;
    }

    @Override
    // MVP 空实现，不写缓存。
    public void put(String key, Object value, Duration ttl) {
        //no op
    }

    @Override
    // MVP 空实现，不删除缓存。
    public void evict(String key) {
        //no op
    }

    @Override
    // MVP 空实现，默认不存在。
    public boolean exists(String key) {
        return false;
    }
}
