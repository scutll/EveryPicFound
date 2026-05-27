package com.everypicfound.common.ratelimit;
public interface RateLimiter {

    // MVP 默认放行所有请求。
    RateLimitResult tryAcquire(RateLimitContext context);
}
