package com.everypicfound.common.log;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
@Data
@ConfigurationProperties(prefix = "everypicfound.log")
public class LogProperties {

    /**
     * 是否启用事件日志。
     *
     * <p>
     * 错误日志始终启用，不提供关闭开关。
     * </p>
     */
    private boolean eventEnabled = true;

    /**
     * 日志根目录。
     */
    private String path = "./logs";

    /**
     * 单个日志文件最大容量。
     */
    private String maxFileSize = "100MB";

    /**
     * 历史日志保留天数。
     */
    private int maxHistory = 30;

    /**
     * 所有历史日志总容量限制。
     */
    private String totalSizeCap = "5GB";
}
