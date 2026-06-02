package com.everypicfound.system.health;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.system-health")
public class SystemHealthProperties {
 
    private Boolean scheduledEnabled = true;

    private Long checkIntervalMs = 60000L;

    private Long initialDelayMs = 30000L;

    private Boolean startupFailFast = false;
}
