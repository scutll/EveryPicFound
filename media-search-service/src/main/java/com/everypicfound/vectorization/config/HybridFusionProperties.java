package com.everypicfound.vectorization.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.vectorization.hybrid-fusion")
public class HybridFusionProperties {

    private Float imageWeight = 0.5f;

    private Float textWeight = 0.5f;

    private Boolean normalizeEnabled = true;
}