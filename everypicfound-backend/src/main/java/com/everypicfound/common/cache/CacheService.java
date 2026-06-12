package com.everypicfound.common.cache;

import java.time.Duration;


public interface CacheService {

    /**
     * 根据 key 获取缓存。
     *
     * @param key       缓存 key
     * @param valueType 期望返回类型
     * @return 命中则返回指定类型对象，未命中返回 null
     */
    <T> T get(String key, Class<T> valueType);

    /**
     * 写入缓存。
     * 
     * @param key   缓存 key
     * @param value 缓存值
     * @param ttl   过期时间；允许为空，由实现类自行决定默认策略
     * @see key对每个模块的统一范式epf:{env}:{module}:{biz}:{detail}
     */
    void put(String key, Object value, Duration ttl);

    /**
     * 删除缓存。
     *
     * @param key 缓存 key
     */
    void evict(String key);

    /**
     * 判断 key 是否存在。
     *
     * @param key 缓存 key
     * @return 存在返回 true，否则返回 false
     */
    boolean exists(String key);
}
