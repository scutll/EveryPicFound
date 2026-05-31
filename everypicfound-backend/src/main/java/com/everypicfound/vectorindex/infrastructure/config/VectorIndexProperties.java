package com.everypicfound.vectorindex.infrastructure.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.everypicfound.vectorindex.collection.VectorCollectionConfig;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "everypicfound.vector-index")
public class VectorIndexProperties {
    
    private String activeCollectionName;

    private Map<String, VectorCollectionConfig> collections = new HashMap<>();
}
