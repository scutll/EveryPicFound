package com.everypicfound.common.cache;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "everypicfound.cache", name = "enabled", havingValue = "true")
public class RedisCacheService implements CacheService {

    private static final String MODULE = "common";

    private static final String BIZ_TYPE = "CACHE";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final CacheProperties cacheProperties;

    private final LogService logService;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T> T get(String key, Class<T> valueType) {
        validateKey(key);
        Assert.notNull(valueType, "Cache value must be not null");

        long startTime = System.currentTimeMillis();

        try {
            String jsonValue = redisTemplate.opsForValue().get(key);

            if (jsonValue == null) {
                return null;
            }

            return objectMapper.readValue(jsonValue, valueType);
        } catch (JsonProcessingException e) {
            recordError("get", LogEventName.CACHE_GET_FAILED, "CACHE_DESERIALIZE_FAILED", key, startTime, e);

            evictCorruptedValue(key);
            return null;
        } catch (DataAccessException e) {
            recordError("get", LogEventName.CACHE_GET_FAILED, "REDIS_ACCESS_FAILED", key, startTime, e);

            return null;
        }

    }

    @Override
    public void put(String key, Object value, Duration tll) {
        validateKey(key);
        Assert.notNull(value, "Cache value must be not null");

        long startTime = System.currentTimeMillis();
        Duration actualTtl = resolveTtl(tll);

        try {
            String jsonValue = objectMapper.writeValueAsString(value);

            redisTemplate.opsForValue().set(key, jsonValue, actualTtl);
        } catch (JsonProcessingException e) {
            recordError(
                    "put",
                    LogEventName.CACHE_PUT_FAILED,
                    "CACHE_SERIALIZE_FAILED",
                    key,
                    startTime,
                    e);
        } catch (DataAccessException e) {
            recordError(
                    "put",
                    LogEventName.CACHE_PUT_FAILED,
                    "REDIS_ACCESS_FAILED",
                    key,
                    startTime,
                    e);
        }
    }

    @Override
    public void evict(String key) {
        validateKey(key);

        long startTime = System.currentTimeMillis();

        try {
            redisTemplate.delete(key);
        } catch (DataAccessException exception) {
            recordError(
                    "evict",
                    LogEventName.CACHE_EVICT_FAILED,
                    "REDIS_ACCESS_FAILED",
                    key,
                    startTime,
                    exception);
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);

        long startTime = System.currentTimeMillis();

        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (DataAccessException exception) {
            recordError(
                    "exists",
                    LogEventName.CACHE_EXISTS_FAILED,
                    "REDIS_ACCESS_FAILED",
                    key,
                    startTime,
                    exception);

            return false;
        }
    }

    private void validateKey(String key) {
        Assert.hasText(key, "Cache key must not be blank");
    }

    private String buildErrorMessage(String key, Exception exception) {
        return "key=" + safe(key)
                + ", exceptionType=" + exception.getClass().getSimpleName()
                + ", exceptionMessage=" + safe(exception.getMessage());
    }

    private void evictCorruptedValue(String key) {
        long startTime = System.currentTimeMillis();

        try {
            redisTemplate.delete(key);
        } catch (DataAccessException e) {
            recordError("evict-corrupted-value", LogEventName.CACHE_EVICT_FAILED, key, key, startTime, e);
        }
    }

    private Duration resolveTtl(Duration ttl) {
        Duration actualTtl = ttl == null
                ? cacheProperties.getDefaultTtl()
                : ttl;

        if (actualTtl.isZero() || actualTtl.isNegative()) {
            throw new IllegalArgumentException("cache ttl must be positive");
        }

        return actualTtl;
    }

    private void recordError(
            String operation,
            LogEventName eventName,
            String errorCode,
            String key,
            long startTime,
            Exception exception) {

        logService.recordErrorLog(
                LogContext.builder()
                        .module(MODULE)
                        .bizType(BIZ_TYPE)
                        .operation(operation)
                        .eventName(eventName.name())
                        .status("FAILED")
                        .costMs(System.currentTimeMillis() - startTime)
                        .errorCode(errorCode)
                        .message(buildErrorMessage(key, exception))
                        .build());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\r", " ").replace("\n", " ");
    }
}
