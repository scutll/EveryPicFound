package com.everypicfound.system.health;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 健康检查调用来源。
 *
 * <p>
 * 使用枚举约束 source 标签取值，
 * 避免业务代码直接传入任意字符串。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum HealthCheckSource {

    /**
     * 应用启动阶段健康检查。
     */
    STARTUP("startup"),

    /**
     * 定时健康检查。
     */
    SCHEDULED("scheduled"),

    /**
     * 主动调用或首次读取快照时触发。
     */
    ON_DEMAND("on_demand");

    private final String value;
}