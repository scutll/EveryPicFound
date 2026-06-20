package com.everypicfound.common.metric;


/**
 * 系统统一指标记录门面。
 *
 * <p>
 * 业务模块不直接依赖 MeterRegistry、Counter 或 Timer。
 * </p>
 */
public interface MetricRecorder {
    
    /**
     * 计数器增加 1。
     */
    void increment(MetricName metricName, MetricTags tags);

    /**
     * 计数器增加指定数量。
     */
    void increment(MetricName metricName, double amount, MetricTags tags);

    /**
     * 记录耗时，单位毫秒。
     */
    void recordTimer(MetricName metricName, Long costMs, MetricTags tags);


    /**
     * 记录非时间类数值样本。
     */
    void recordValue(MetricName metricName, Number value, MetricTags tags);
}
