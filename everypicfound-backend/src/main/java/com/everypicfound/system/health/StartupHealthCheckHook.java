package com.everypicfound.system.health;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class StartupHealthCheckHook implements ApplicationRunner{
    
    private final SystemHealthCheckService systemHealthCheckService;

    private final SystemHealthProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        SystemHealthSnapshot snapshot = systemHealthCheckService.check(true);

        if (Boolean.TRUE.equals(properties.getStartupFailFast()) && !Boolean.TRUE.equals(snapshot.getHealthy())) {
            throw new IllegalStateException("system health check failed on startup: " + snapshot);
        }
    }
}
