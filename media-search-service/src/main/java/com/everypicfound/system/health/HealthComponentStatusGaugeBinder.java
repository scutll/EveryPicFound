package com.everypicfound.system.health;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricTag;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * 健康组件实时状态 Gauge。
 *
 * <p>
 * 指标值：
 * </p>
 *
 * <ul>
 * <li>1：组件当前健康；</li>
 * <li>0：组件当前不可用。</li>
 * </ul>
 *
 * <p>
 * Gauge 保存 AtomicInteger 引用，在监控系统采集时
 * 读取组件的最新状态。
 * </p>
 */
@Component
public class HealthComponentStatusGaugeBinder implements MeterBinder {
    
    private static final int STATUS_UP = 1;

    private static final int STATUS_DOWN = 0;

    private final Map<HealthComponent, AtomicInteger> componentStatusValues = new EnumMap<>(HealthComponent.class);

    public HealthComponentStatusGaugeBinder() {
        for (HealthComponent component : HealthComponent.values()) {

            /*
             * 应用完成第一次启动检查前，
             * 暂时按不可用状态初始化。
             */
            componentStatusValues.put(component, new AtomicInteger(STATUS_DOWN));
        }
    }
    

    @Override
    public void bindTo(MeterRegistry registry) {
        componentStatusValues.forEach(
                (component, statusValue) -> registerGauge(
                        registry,
                        component,
                        statusValue));
    }

    /**
     * 更新指定组件的当前状态。
     */
    public void updateStatus(HealthComponent component, boolean healthy) {
        if (component == null) {
            return;
        }

        AtomicInteger statusValue = componentStatusValues.get(component);

        if (statusValue == null) {
            return;
        }

        statusValue.set(
                healthy
                        ? STATUS_UP
                        : STATUS_DOWN);
    }

    /**
     * 获取当前状态，主要用于测试。
     */
    public int getStatus(HealthComponent component) {
        AtomicInteger statusValue = componentStatusValues.get(component);

        return statusValue == null ? STATUS_DOWN : statusValue.get();
    }


    private void registerGauge(MeterRegistry registry, HealthComponent component, AtomicInteger statusValue) {
        Gauge.builder(MetricName.HEALTH_COMPONENT_STATUS.getMetricName(), statusValue, AtomicInteger::get)
            .description(MetricName.HEALTH_COMPONENT_STATUS.getDescription())
            .tag(MetricTag.COMPONENT.getKey(), component.getValue())
                .register(registry);
    }
}
