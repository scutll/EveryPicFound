package com.everypicfound.common.metric.binder;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.ToDoubleFunction;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.everypicfound.common.executor.ThreadPoolManager;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricTag;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.Gauge;
import lombok.RequiredArgsConstructor;

/**
 * 业务线程池实时状态指标绑定器。
 *
 * <p>
 * Gauge 是实时状态指标。监控系统采集时，
 * Micrometer 会调用 ThreadPoolExecutor 的对应方法，
 * 读取线程池当时的实际状态。
 * </p>
 *
 * <p>
 * 当前系统的向量化等异步任务共用 commonExecutor，
 * 因此 executor 标签固定为 common。
 * 后续真正拆分独立线程池后，再分别注册对应 Binder。
 * </p>
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "everypicfound.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ExecutorGaugeBinder implements MeterBinder {

    private static final String EXECUTOR_NAME = ThreadPoolManager.COMMON_EXECUTOR_NAME;

    private final ThreadPoolManager threadPoolManager;

    /**
     * Spring Boot 创建 MeterRegistry 后，
     * 会自动调用所有 MeterBinder Bean 的 bindTo 方法。
     *
     * @param registry Micrometer 指标注册中心
     */
    @Override
    public void bindTo(MeterRegistry registry) {
        ThreadPoolExecutor executor = threadPoolManager.getCommonThreadPoolExecutor();

        registerGauge(
                registry,
                MetricName.EXECUTOR_ACTIVE,
                executor,
                ThreadPoolExecutor::getActiveCount);

        registerGauge(
                registry,
                MetricName.EXECUTOR_POOL_SIZE,
                executor,
                ThreadPoolExecutor::getPoolSize);

        registerGauge(
                registry,
                MetricName.EXECUTOR_MAX_POOL_SIZE,
                executor,
                ThreadPoolExecutor::getMaximumPoolSize);

        registerGauge(
                registry,
                MetricName.EXECUTOR_QUEUE_SIZE,
                executor,
                value -> value.getQueue().size());

        registerGauge(
                registry,
                MetricName.EXECUTOR_QUEUE_REMAINING_CAPACITY,
                executor,
                value -> value.getQueue()
                        .remainingCapacity());

        registerGauge(
                registry,
                MetricName.EXECUTOR_COMPLETED_TASKS,
                executor,
                value -> value.getCompletedTaskCount());
    }

    /**
     * 统一注册线程池 Gauge，保证每个指标使用相同标签口径。
     */
    private void registerGauge(
            MeterRegistry registry,
            MetricName metricName,
            ThreadPoolExecutor executor,
            ToDoubleFunction<ThreadPoolExecutor> valueFunction) {

        Gauge.builder(metricName.getMetricName(), executor, valueFunction)
                .description(metricName.getDescription())
                .tag(MetricTag.EXECUTOR.getKey(), EXECUTOR_NAME)
                .register(registry);
    }
}
