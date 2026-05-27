package com.everypicfound.common.executor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcurrencyProperties {

    // 核心线程数。
    private Integer corePoolSize;

    // 最大线程数。
    private Integer maxPoolSize;

    // 队列长度。
    private Integer queueCapacity;

    // 拒绝策略。
    private String rejectionPolicy;
}
