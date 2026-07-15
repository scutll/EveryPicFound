package com.everypicfound.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitContext {

    // 当前模块。
    private String module;

    // 当前操作。
    private String operation;

    // 用户 ID。
    private String userId;

    // 客户端 IP。
    private String clientIp;
}
