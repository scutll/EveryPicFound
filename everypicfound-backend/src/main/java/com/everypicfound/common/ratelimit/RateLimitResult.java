package com.everypicfound.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {

    // 是否允许通过限流。
    private Boolean allowed;

    // 异常携带的错误码。
    private String errorCode;

    // 响应消息。
    private String message;
}
