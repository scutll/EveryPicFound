package com.everypicfound.common.cache;

import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.everypicfound.common.cache.error.CacheErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
@ConditionalOnProperty(prefix = "everypicfound.cache", name = "enabled", havingValue = "true")
public class RedisCacheService implements CacheService {

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private final CacheProperties cacheProperties;

    private final MetricRecorder metricRecorder;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public <T> T get(String key, Class<T> valueType) {
        validateKey(key);
        Assert.notNull(valueType, "Cache value must be not null");

        long startTime = System.currentTimeMillis();
        String result = "failed";

        try {
            String jsonValue = redisTemplate.opsForValue().get(key);

            if (jsonValue == null) {
                result = "miss";
                return null;
            }
            T value = objectMapper.readValue(jsonValue, valueType);
            result = "success";
            return value;

        } catch (JsonProcessingException exception) {
            result = "deserialize_failed";

            removeCorruptedValue(key, exception);
            throw new SystemException(CacheErrorCode.CACHE_DESERIALIZE_FAILED, exception);
        } catch (DataAccessException exception) {
            result = "failed";

            throw new SystemException(CacheErrorCode.REDIS_ACCESS_FAILED, exception);
        } finally {
            recordRedisMetrics("get", result, startTime);
        }

    }

    private void removeCorruptedValue(
            String key,
            JsonProcessingException deserializeException) {

        try {
            redisTemplate.delete(key);
        } catch (DataAccessException cleanupException) {
            /*
             * 反序列化失败是主异常；
             * 删除损坏缓存失败是处理主异常时发生的次要异常。
             * 也是抛出异常的一种
             */
            deserializeException.addSuppressed(
                    cleanupException);
        }
    }

    @Override
    public void put(String key, Object value, Duration ttl) {
        validateKey(key);
        Assert.notNull(value, "Cache value must be not null");

        long startTime = System.currentTimeMillis();
        String result = "failed";
        
        try {
            Duration actualTtl = resolveTtl(ttl);
            String jsonValue = objectMapper.writeValueAsString(value);

            redisTemplate.opsForValue().set(key, jsonValue, actualTtl);
            result = "success";
        } catch (JsonProcessingException exception) {
            result = "serialize_failed";
            
            throw new SystemException(CacheErrorCode.CACHE_SERIALIZE_FAILED, exception);
        } catch (DataAccessException exception) {
            result = "failed";

            throw new SystemException(CacheErrorCode.REDIS_ACCESS_FAILED, exception);
        } finally {
            recordRedisMetrics("put", result, startTime);
        }
    }

    @Override
    public void evict(String key) {
        validateKey(key);

        long startTime = System.currentTimeMillis();
        String result = "failed";

        try {
            redisTemplate.delete(key);
            result = "success";
        } catch (DataAccessException exception) {
            result = "failed"; 

            throw new SystemException(CacheErrorCode.REDIS_ACCESS_FAILED, exception);
        } finally {
            recordRedisMetrics("evict", result, startTime);
        }
    }

    @Override
    public boolean exists(String key) {
        validateKey(key);

        long startTime = System.currentTimeMillis();
        String result = "failed";
        try {
            Boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(key));
            result = Boolean.toString(exists);
            return exists;
        } catch (DataAccessException exception) {
            result = "failed";

            throw new SystemException(
                    CacheErrorCode.REDIS_ACCESS_FAILED,
                    exception);
        } finally {
            recordRedisMetrics("exists", result, startTime);
        }
    }

    private void validateKey(String key) {
        Assert.hasText(key, "Cache key must not be blank");
    }


    private Duration resolveTtl(Duration ttl) {
        Duration actualTtl = ttl == null
                ? cacheProperties.getDefaultTtl()
                : ttl;

        if (actualTtl.isZero() || actualTtl.isNegative()) {
            throw new SystemException(CacheErrorCode.CACHE_TTL_INVALID);
        }

        return actualTtl;
    }
    
    private void recordRedisMetrics(
                String operation,
                String result,
                long startTime
    ) {
        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.OPERATION, operation)
                .tag(MetricTag.RESULT, result)
                .build();
        
        metricRecorder.increment(MetricName.REDIS_OPERATIONS, tags);

        metricRecorder.recordTimer(MetricName.REDIS_DURATION, System.currentTimeMillis() - startTime, tags);
    }

}
