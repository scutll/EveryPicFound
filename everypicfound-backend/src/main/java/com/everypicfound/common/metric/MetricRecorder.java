package com.everypicfound.common.metric;
public interface MetricRecorder {

    // 记录次数类指标。
    void increment(MetricName metricName, MetricTags tags);

    // 记录耗时类指标。
    void recordTimer(MetricName metricName, long costMs, MetricTags tags);

    // 记录数值类指标。
    void recordValue(MetricName metricName, double value, MetricTags tags);
}
