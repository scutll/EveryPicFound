package com.everypicfound.common.ratelimit;
public enum RateLimitType {

    // 接口级限流。
    API,

    // 用户级限流。
    USER,

    // IP 级限流。
    IP
}
