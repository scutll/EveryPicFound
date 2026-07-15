package com.everypicfound.system.health;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 系统健康检查组件。
 */
@Getter
@RequiredArgsConstructor
public enum HealthComponent {

    MYSQL("mysql"),

    QDRANT("qdrant"),

    MODEL_SERVICE("model_service");

    /**
     * 用于指标标签和结构化日志的规范名称。
     */
    private final String value;
}