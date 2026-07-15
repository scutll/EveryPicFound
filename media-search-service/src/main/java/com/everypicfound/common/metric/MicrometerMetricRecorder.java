package com.everypicfound.common.metric;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Micrometer 的指标记录实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "everypicfound.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MicrometerMetricRecorder implements MetricRecorder {
    
    // Micrometer采用懒注册，registry发现新指标的时候才进行创建
    private final MeterRegistry meterRegistry;

    /**
     * 指标系统发生持续异常时只记录第一条警告，
     * 防止每次业务调用都产生重复日志。
     */
    private final AtomicBoolean warningLogged = new AtomicBoolean(false);

    @Override
    public void increment(MetricName metricName, MetricTags metricTags) {
        increment(metricName, 1D, metricTags);
    }

    @Override
    public void increment(MetricName metricName, double amount, MetricTags tags) {
        if (!isExpectedType(metricName, MetricType.COUNTER)) {
            return;
        }

        if (!Double.isFinite(amount) || amount <= 0D) {
            return;
        }

        recordSafely(metricName, () -> {
            Counter counter = Counter.builder(metricName.getMetricName())
                    .description(metricName.getDescription())
                    .tags(toMicrometerTags(tags))
                    .register(meterRegistry); //注册器，若没有注册则先用这个注册器进行注册
            counter.increment(amount);

        });
    }
    
    @Override
    public void recordTimer(MetricName metricName, Long costMs, MetricTags tags) {
        if (!isExpectedType(metricName, MetricType.TIMER)) {
            return;
        }

        /*
         * null 和负耗时表示采集数据异常。
         * 不能用 0 兜底，否则会污染真实延迟分布。
         */
        if (costMs == null || costMs < 0L) {
            return;
        }

        recordSafely(metricName, () -> {
            Timer timer = Timer.builder(metricName.getMetricName())
                    .description(metricName.getDescription())
                    .tags(toMicrometerTags(tags))
                    .register(meterRegistry);

            timer.record(costMs, TimeUnit.MILLISECONDS);
        });
    }

    @Override
    public void recordValue(MetricName metricName, Number value, MetricTags tags) {
        if (!isExpectedType(metricName, MetricType.DISTRIBUTION_SUMMARY)) {
            return;
        }

        if (value == null) {
            return;
        }

        double doubleValue = value.doubleValue();

        if (!Double.isFinite(doubleValue) || doubleValue < 0D) {
            return;
        }

        recordSafely(metricName, () -> {
            DistributionSummary.Builder builder = DistributionSummary.builder(metricName.getMetricName())
                .description(metricName.getDescription())
                    .tags(toMicrometerTags(tags));

            if (metricName.getBaseUnit() != null && !metricName.getBaseUnit().isBlank()) {
                builder.baseUnit(metricName.getBaseUnit());
            }

            builder.register(meterRegistry).record(doubleValue);
        });
    }



    private boolean isExpectedType(MetricName metricName,
            MetricType expectedType) {
        if (metricName == null) {
            warnOnce(
                    "Metric recording ignored because metricName is null",
                    null,
                    null);
            return false;
        }

        if (metricName.getMetricType() != expectedType) {
            warnOnce(
                    "Metric type mismatch, expected="
                            + expectedType
                            + ", actual="
                            + metricName.getMetricType(),
                    metricName,
                    null);
            return false;
        }

        return true;
    }

    private Tags toMicrometerTags(MetricTags metricTags) {
        if (metricTags == null || metricTags.isEmpty()) {
            return Tags.empty();
        }

        List<Tag> tags = new ArrayList<>();

        for (Map.Entry<String, String> entry : metricTags.getTags().entrySet()) {
            tags.add(Tag.of(entry.getKey(), entry.getValue()));
        }

        return Tags.of(tags);
    }

    /**
     * 指标采集不能影响主业务流程。
     */
    private void recordSafely(MetricName metricName, Runnable action) {
        try{
            action.run();
        } catch (RuntimeException exception) {
            warnOnce("Metric record failed", metricName, exception);
        }
    } 

    private void warnOnce(String message,
                        MetricName metricName,
            Throwable throwable) {
    
        
        // 只在第一次warningLogger为false的时候才进行warning并设为true
        if (!warningLogged.compareAndSet(false, true)) {
            return;
        }

        String metricNameValue = metricName == null
                ? "unKnown"
                : metricName.getMetricName();
        
        if (throwable == null) {
            log.warn("{}, metricName={}", message, metricNameValue);
            return;
        }

        log.warn("{}, metricName={}", message, metricNameValue, throwable);

    }


}
