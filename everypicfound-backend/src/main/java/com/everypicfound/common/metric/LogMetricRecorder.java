package com.everypicfound.common.metric;

import java.util.Comparator;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LogMetricRecorder implements MetricRecorder {

    @Override
    // 记录指标
    public void increment(MetricName metricName, MetricTags tags) {
        log.info("metric type=count, name={}, value=1, tags={}", formatMetricName(metricName), formatTags(tags));
        // 这句的原理是：将 tags 中的键值对转换成字符串，并使用逗号分隔，例如：tag1=value1,tag2=value2。
    }

    @Override
    public void recordTimer(MetricName metricName, Long costMs, MetricTags tags) {
         // codex: 耗时指标即使调用方传入 null，也不能打断上传链路；日志中用 0 兜底便于排查。
        long safeCostMs = costMs == null ? 0L : costMs;
        log.info("metric type=timer, name={}, costMs={}, tags={}", formatMetricName(metricName), safeCostMs, formatTags(tags));
    }

    @Override
    public void recordValue(MetricName metricName, Number value, MetricTags tags) {
         // codex: value 指标可能来自文件大小、队列长度等数值，null 时用 0 兜底，避免指标记录影响主流程。
        Number safeValue = value == null ? 0 : value;
        log.info("metric type=value, name={}, value={}, tags={}", formatMetricName(metricName), safeValue, formatTags(tags));
    }

    // 格式化指标名称
    private String formatMetricName(MetricName metricName) {
        if (metricName == null) {
            // codex: 指标名称为空时用 UNKNOWN 兜底，避免记录指标时影响主流程。
            return "unknown";
        }
        // codex: 文档示例使用 search_total_count 这种小写下划线指标名，日志输出保持同一风格。
        return metricName.name().toLowerCase(Locale.ROOT);
    }

    // 格式化标签为字符串
    private String formatTags(MetricTags metricTags) {
        if (metricTags == null || metricTags.getTags() == null || metricTags.getTags().isEmpty()) {
            return "";
        }

        return metricTags.getTags().entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .sorted(Comparator.comparing(Map.Entry::getKey)) // 按照标签键排序，保证输出顺序一致
                .map(entry -> safe(entry.getKey()) + "=" + safe(entry.getValue())) // 转换为 "key=value" 格式
                .collect(Collectors.joining(",")); // 使用逗号分隔标签
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        // codex: 指标标签通常会被日志系统检索，去掉换行可以避免一条指标日志被拆成多行。
        return value.replace("\r", " ").replace("\n", " ");
    }
}
