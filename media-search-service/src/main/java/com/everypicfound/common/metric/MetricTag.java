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
    

    /**
     * 业务模块。
     *
     * <p>
     * 例如 search、storage、vectorization。
     * </p>
     */
    MODULE("module"),

    /**
     * 操作名称。
     *
     * <p>
     * 例如 get、put、save、read、search。
     * </p>
     */
    OPERATION("operation"),

    /**
     * 操作结果。
     *
     * <p>
     * 例如 success、failed、rejected、skipped。
     * </p>
     */
    RESULT("result"),

    /**
     * 当前状态。
     *
     * <p>
     * 主要用于健康状态、任务状态等状态型指标。
     * </p>
     */
    STATUS("status"),

    /**
     * 用例内部阶段。
     *
     * <p>
     * 例如 validate、vector_recall、backfill、filter。
     * </p>
     */
    STAGE("stage"),

    /**
     * 搜索类型。
     *
     * <p>
     * 固定值为 image、text、hybrid。
     * </p>
     */
    SEARCH_TYPE("search_type"),

    /**
     * 向量化类型。
     *
     * <p>
     * 固定值为 image、text。
     * </p>
     */
    VECTORIZATION_TYPE("vectorize_type"),

    /**
     * 调用来源。
     *
     * <p>
     * 例如 search、image_upload、scanner、health_check。
     * </p>
     */
    SOURCE("source"),

    /**
     * 外部依赖。
     *
     * <p>
     * 例如 mysql、redis、qdrant、model_service。
     * </p>
     */
    DEPENDENCY("dependency"),

    /**
     * 缓存逻辑名称。
     *
     * <p>
     * 例如 search_result、text_vector。
     * </p>
     */
    CACHE_NAME("cache_name"),

    /**
     * 缓存访问结果。
     *
     * <p>
     * 例如 hit、miss、error、not_applicable。
     * </p>
     */
    CACHE_RESULT("cache_result"),

    /**
     * 健康检查组件。
     *
     * <p>
     * 例如 mysql、qdrant、model_service。
     * </p>
     */
    COMPONENT("component"),

    /**
     * 线程池名称。
     *
     * <p>
     * 例如 vectorization。
     * </p>
     */
    EXECUTOR("executor"),

    /**
     * 外部接口端点。
     *
     * <p>
     * 例如 vectorize_image、vectorize_text、health。
     * </p>
     */
    ENDPOINT("endpoint"),
                    
    /**
     * 有限枚举的失败或处理原因。
     *
     * <p>
     * 只能使用错误码、FailReason 或固定枚举值，
     * 不得使用 exception.getMessage()。
     * </p>
     */
    REASON("reason");

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
