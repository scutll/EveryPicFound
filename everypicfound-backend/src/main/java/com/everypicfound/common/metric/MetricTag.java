package com.everypicfound.common.metric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


/**
 * 系统允许使用的指标标签。
 *
 * <p>
 * 禁止业务模块随意创建标签名，避免 requestId、imageId 等
 * 高基数字段进入指标系统。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum MetricTag {
    

    MODULE("module"),

    OPERATION("operation"),

    RESULT("result"),

    STATUS("status"),

    SEARCH_TYPE("search_type"),

    DEPENDENCY("dependency"),

    REASON("reason"),

    SOURCE("source"),

    CACHE_RESULT("cache_result");

    private final String key;

    /**
     * 根据标签字符串查找标准标签。
     *
     * @param key 标签名
     * @return 对应枚举，不存在时返回 null
     */
    public static MetricTag fromKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        for (MetricTag metricTag : values()) {
            if (metricTag.key.equals(key.trim())) {
                return metricTag;
            }
        }

        return null;
    }
}
