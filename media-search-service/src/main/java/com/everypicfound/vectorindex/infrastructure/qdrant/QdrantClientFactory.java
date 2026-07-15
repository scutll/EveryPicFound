package com.everypicfound.vectorindex.infrastructure.qdrant;

import java.time.Duration;

import org.springframework.stereotype.Component;

import com.everypicfound.vectorindex.infrastructure.config.QdrantProperties;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class QdrantClientFactory {
    
    private final QdrantProperties properties;

    public QdrantClient createClient() {
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
            properties.getHost(),
            properties.getPort(),
            Boolean.TRUE.equals(properties.getUseTls())
        );

        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            builder.withApiKey(properties.getApiKey());
        }

        if (properties.getTimeoutMs() != null && properties.getTimeoutMs() > 0) {
            builder.withTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        }

        return new QdrantClient(builder.build());
        
    }

}
