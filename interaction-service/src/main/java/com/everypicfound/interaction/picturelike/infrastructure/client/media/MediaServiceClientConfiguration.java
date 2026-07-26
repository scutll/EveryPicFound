package com.everypicfound.interaction.picturelike.infrastructure.client.media;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MediaServiceClientProperties.class)
public class MediaServiceClientConfiguration {

    @Bean
    @Qualifier("mediaServiceRestClient")
    RestClient mediaServiceRestClient(
            RestClient.Builder builder,
            MediaServiceClientProperties properties) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(
                requiredDuration(
                        properties.connectTimeout(),
                        "connect timeout"));
        requestFactory.setReadTimeout(
                requiredDuration(
                        properties.readTimeout(),
                        "read timeout"));
        return builder
                .baseUrl(requiredBaseUrl(properties).toString())
                .requestFactory(requestFactory)
                .requestInterceptor(
                        new BearerTokenForwardingInterceptor())
                .build();
    }

    private java.net.URI requiredBaseUrl(
            MediaServiceClientProperties properties) {
        if (properties.baseUrl() == null) {
            throw new IllegalStateException(
                    "media service base URL is required");
        }
        return properties.baseUrl();
    }

    private Duration requiredDuration(
            Duration duration,
            String name) {
        if (duration == null
                || duration.isZero()
                || duration.isNegative()) {
            throw new IllegalStateException(
                    "media service " + name + " must be positive");
        }
        return duration;
    }
}
