package com.everypicfound.identity.infrastructure.config.bean;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * 应用时间源配置。
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
