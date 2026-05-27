package com.everypicfound.common.metric;

import org.springframework.stereotype.Service;

@Service
public class LogMetricRecorder implements MetricRecorder {

    @Override
    public void increment(MetricName metricName, MetricTags tags) {
    }

    @Override
    public void recordTimer(MetricName metricName, Long costMs, MetricTags tags) {
    }

    @Override
    public void recordValue(MetricName metricName, Number value, MetricTags tags) {
    }
}