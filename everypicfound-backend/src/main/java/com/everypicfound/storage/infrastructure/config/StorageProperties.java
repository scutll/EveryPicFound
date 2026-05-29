package com.everypicfound.storage.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
@Data
@Component
@ConfigurationProperties(prefix = "everypicfound.storage")
public class StorageProperties {

    // 本地文件存储根目录。
    private String basePath;

    // 文件访问 URL 前缀。
    private String accessUrlPrefix;

    // 最大文件大小限制，可供 image-asset 校验使用。
    private Long maxFileSize;

    // 允许的文件扩展名，可供 image-asset 校验使用。
    private List<String> allowedExtensions;
}
