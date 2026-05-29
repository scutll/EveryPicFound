package com.everypicfound.modelclient.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.model-client")
public class ModelClientProperties {
    /**
     * Python 模型服务基础地址。
     */
    private String baseUrl = "http://127.0.0.1:8001";

    /**
     * 图片向量化接口路径。
     */
    private String imageVectorizePath = "/vectorize/image";

    /**
     * 文本向量化接口路径。
     */
    private String textVectorizePath = "/vectorize/text";

    /**
     * 健康检查接口路径。
     */
    private String healthPath = "/health";

    /**
     * 连接超时时间，单位毫秒。
     */
    private Integer connectTimeoutMs = 3000;

    /**
     * 读取超时时间，单位毫秒。
     */
    private Integer readTimeoutMs = 60000;

    /**
     * 默认模型名称。
     */
    private String defaultModelName = "clip-ViT-32B";
}
