package com.everypicfound.common.metric;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 禁用指标功能时使用的空实现。
 */
@Service
@ConditionalOnProperty(
        prefix = "everypicfound.metrics",
        name = "enabled",
        havingValue = "false")
public class NoOpMetricRecorder implements MetricRecorder {

    @Override
    public void increment(MetricName metricName,
                          MetricTags tags) {
        // No operation.
    }

    @Override
    public void increment(MetricName metricName,
                          double amount,
                          MetricTags tags) {
        // No operation.
    }

    @Override
    public void recordTimer(MetricName metricName,
                            Long costMs,
                            MetricTags tags) {
        // No operation.
    }

    @Override
    public void recordValue(MetricName metricName,
                            Number value,
                            MetricTags tags) {
        // No operation.
    }
}
