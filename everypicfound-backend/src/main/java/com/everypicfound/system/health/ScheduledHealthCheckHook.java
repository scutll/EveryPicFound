package com.everypicfound.system.health;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ScheduledHealthCheckHook {
    
    private final SystemHealthCheckService systemHealthCheckService;

    private final SystemHealthProperties properties;

    @Scheduled(
        fixedDelayString = "${everypicfound.system-health.check-interval-ms:60000}",
            initialDelayString = "${everypicfound.system-health.initial-delay-ms:30000}"
    )
    public void check() {
        if (!Boolean.TRUE.equals(properties.getScheduledEnabled())) {
            return;
        }

        systemHealthCheckService.check(false);
    }
}
