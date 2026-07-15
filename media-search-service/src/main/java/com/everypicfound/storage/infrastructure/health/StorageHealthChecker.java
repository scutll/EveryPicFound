package com.everypicfound.storage.infrastructure.health;

import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.storage.infrastructure.config.StorageProperties;
public class StorageHealthChecker {

    // 存储配置。
    private StorageProperties storageProperties;

    // 记录存储健康检查日志。
    private LogService logService;

    // 记录存储健康指标。
    private MetricRecorder metricRecorder;

    // 检查本地存储目录是否存在、是否可读写。
    public void check() {
        throw new UnsupportedOperationException("TODO");
    }
}
