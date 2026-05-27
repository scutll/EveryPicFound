package com.everypicfound.common.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "everypicfound.log")
public class LogProperties {

    // 是否开启慢日志。
    private Boolean slowLogEnabled;

    // 慢请求阈值。
    private Long slowRequestThresholdMs;
}
