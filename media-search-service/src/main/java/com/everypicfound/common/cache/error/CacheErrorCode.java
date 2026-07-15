package com.everypicfound.common.cache.error;

import com.everypicfound.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 缓存与 Redis 基础设施错误码。
 *
 * <p>
 * 缓存未命中不是错误，不在本枚举中定义。
 * 只有序列化、反序列化、Redis 访问和内部配置异常才属于系统错误。
 * </p>
 */
@Getter
@AllArgsConstructor
public enum CacheErrorCode implements ErrorCode {

    CACHE_SERIALIZE_FAILED(
            100001,
            "cache value serialization failed"),

    CACHE_DESERIALIZE_FAILED(
            100002,
            "cache value deserialization failed"),

    REDIS_ACCESS_FAILED(
            100003,
            "redis access failed"),

    CACHE_TTL_INVALID(
            100004,
            "cache ttl must be positive");

    private final Integer code;

    private final String message;
}