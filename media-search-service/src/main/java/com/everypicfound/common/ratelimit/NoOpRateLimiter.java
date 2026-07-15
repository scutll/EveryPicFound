package com.everypicfound.common.ratelimit;
public class NoOpRateLimiter implements RateLimiter {

    @Override
    // MVP 默认放行所有请求。
    public RateLimitResult tryAcquire(RateLimitContext context) {
        throw new UnsupportedOperationException("TODO");
    }
}
