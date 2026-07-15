package com.everypicfound.vectorindex.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.vector-index.qdrant")
public class QdrantProperties {
    

    private String host = "127.0.0.1";

    private Integer port = 6334;

    private Boolean useTls = false; //gPRC通道是否用TLS加密传输

    private String apiKey;

    private Integer timeoutMs = 10000;
}
