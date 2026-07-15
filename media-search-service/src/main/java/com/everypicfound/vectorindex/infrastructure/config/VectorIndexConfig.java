package com.everypicfound.vectorindex.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.everypicfound.vectorindex.infrastructure.qdrant.QdrantClientFactory;

import io.qdrant.client.QdrantClient;

@Configuration
@EnableConfigurationProperties({
        VectorIndexProperties.class,
        QdrantProperties.class
})
public class VectorIndexConfig {
    

    @Bean
    public QdrantClient qdrantClient(QdrantClientFactory qdrantClientFactory) {
        return qdrantClientFactory.createClient();
    }
}
