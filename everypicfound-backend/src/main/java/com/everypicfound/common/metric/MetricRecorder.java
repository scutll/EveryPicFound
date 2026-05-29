package com.everypicfound.common.metric;

public interface MetricRecorder {
    
    void increment(MetricName metricName, MetricTags tags);

    void recordTimer(MetricName metricName, Long costMs, MetricTags tags);

    void recordValue(MetricName metricName, Number value, MetricTags tags);
}
