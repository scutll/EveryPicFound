package com.everypicfound.common.metric;
public class LogMetricRecorder implements MetricRecorder {

    @Override
    // 记录次数类指标。
    public void increment(MetricName metricName, MetricTags tags) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录耗时类指标。
    public void recordTimer(MetricName metricName, long costMs, MetricTags tags) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    // 记录数值类指标。
    public void recordValue(MetricName metricName, double value, MetricTags tags) {
        throw new UnsupportedOperationException("TODO");
    }
}
