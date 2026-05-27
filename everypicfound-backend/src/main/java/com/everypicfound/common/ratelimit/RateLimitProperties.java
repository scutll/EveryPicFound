package com.everypicfound.common.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitProperties {

    // 配置开关。
    private Boolean enabled;

    // 限流阈值。
    private Integer threshold;

    // 限流时间窗口。
    private Long windowSeconds;
}
