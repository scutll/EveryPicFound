package com.everypicfound.imageasset;

import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.imageasset.interfaces.controller.ImageFileController;
import com.everypicfound.storage.infrastructure.config.StorageProperties;
import com.everypicfound.storage.infrastructure.local.DateBasedStoragePathGenerator;
import com.everypicfound.storage.infrastructure.local.LocalFileStorageService;

@SpringBootConfiguration
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(StorageProperties.class)
@Import({
        ImageFileController.class,
        LocalFileStorageService.class,
        DateBasedStoragePathGenerator.class
})
public class ImageFileTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImageFileTestApplication.class,
                "--server.port=18080",
                "--spring.profiles.active=image-file-test",
                "--everypicfound.storage.base-path=E:/EveryPicFound/everypicfound-backend/data/images",
                "--everypicfound.storage.access-url-prefix=/images");
    }

    @Bean
    public LogService logService() {
        return new LogService() {
            @Override
            public void recordBizLog(LogContext context) {
            }

            @Override
            public void recordSuccessLog(LogContext context) {
            }

            @Override
            public void recordErrorLog(LogContext context) {
            }

            @Override
            public void recordStateChangeLog(LogContext context) {
            }

            @Override
            public void recordSlowLog(LogContext context) {
            }
        };
    }

    @Bean
    public MetricRecorder metricRecorder() {
        return new MetricRecorder() {
            @Override
            public void increment(MetricName metricName, MetricTags tags) {
            }

            @Override
            public void recordTimer(MetricName metricName, Long costMs, MetricTags tags) {
            }

            @Override
            public void recordValue(MetricName metricName, Number value, MetricTags tags) {
            }
        };
    }
}
