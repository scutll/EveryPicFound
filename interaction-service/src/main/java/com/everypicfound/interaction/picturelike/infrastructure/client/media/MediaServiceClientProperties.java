package com.everypicfound.interaction.picturelike.infrastructure.client.media;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "everypicfound.clients.media")
public record MediaServiceClientProperties(
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout) {
}
