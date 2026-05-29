package com.everypicfound.common.cache;

public interface CacheService {

    // 获取当前请求或任务上下文。
    Object get(String key);

    // MVP 空实现，不写缓存。
    void put(String key, Object value);

    // MVP 空实现，不删除缓存。
    void evict(String key);

    // MVP 空实现，默认不存在。
    boolean exists(String key);
}
