package com.everypicfound.common.metric;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "everypicfound.metric")
public class MetricProperties {

    // 配置开关。
    private Boolean enabled;

    // 指标采样率。
    private Double sampleRate;
}
