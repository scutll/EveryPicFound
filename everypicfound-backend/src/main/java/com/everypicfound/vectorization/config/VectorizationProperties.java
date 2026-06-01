package com.everypicfound.vectorization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.vectorization")
public class VectorizationProperties {
    
    private Integer maxRetryCount = 3;

    private Integer processingTimeoutSeconds = 300;

    private VectorizationPublishMode publishMode = VectorizationPublishMode.THREAD_POOL;

    private Boolean scannerEnabled = false;

    private Integer pendingScanBatchSize = 100;
}
