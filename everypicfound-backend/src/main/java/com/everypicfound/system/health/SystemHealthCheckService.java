package com.everypicfound.system.health;

import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;
import java.sql.Connection;

import org.springframework.stereotype.Service;

import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ModelHealthResult;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.collection.health.VectorCollectionHealthChecker;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemHealthCheckService {
    

    private final DataSource dataSource;
    
    private final ModelVectorizationClient modelVectorizationClient;

    private final VectorCollectionHealthChecker vectorCollectionHealthChecker;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final AtomicReference<SystemHealthSnapshot> latestSnapshot = new AtomicReference<>();


    public SystemHealthSnapshot check(boolean ensureVectorCollection) {
        ComponentHealthResult database = checkDatabase();

        ComponentHealthResult vectorIndex = checkVectorIndex(ensureVectorCollection);

        ComponentHealthResult modelService = checkModelService();

        List<ComponentHealthResult> components = List.of(database, vectorIndex, modelService);

        boolean healthy = components.stream()
                .allMatch(item -> Boolean.TRUE.equals(item.getHealthy()));

        SystemHealthSnapshot snapshot = SystemHealthSnapshot.builder()
                .healthy(healthy)
                .checkTime(LocalDateTime.now())
                .components(components)
                .build();

        latestSnapshot.set(snapshot);

        return snapshot;
    }
    
    public SystemHealthSnapshot getLatestSnapshot() {
        SystemHealthSnapshot snapshot = latestSnapshot.get();
        if (snapshot != null) {
            return snapshot;
        }
        return check(false);
    }


    private ComponentHealthResult checkDatabase() {
        long start = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(3);
            return buildResult("mysql", valid, start, valid ? "UP" : "DOWN");
        } catch (Exception e) {
            return buildResult("mysql", false, start, e.getMessage());
        }
    }

    private ComponentHealthResult checkVectorIndex(boolean ensureVectorCollection) {
        long start = System.currentTimeMillis();

        try {
            if (ensureVectorCollection) {
                vectorCollectionHealthChecker.ensureActiveCollectionReady();
            } else {
                vectorCollectionHealthChecker.check();

                VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();
                boolean exists = vectorCollectionHealthChecker.checkCollectionExists(config.getCollectionName());

                if (!exists) {
                    return buildResult("qdrant", false, start, "active collection not exist");
                }
            }

            return buildResult("qdrant", true, start, "UP");

        } catch (Exception e) {
            return buildResult("qdrant", false, start, e.getMessage());
        }
    }

    private ComponentHealthResult checkModelService() {
        long start = System.currentTimeMillis();

        try{
            ModelHealthResult result = modelVectorizationClient.checkHealth();

            boolean healthy = result != null && Boolean.TRUE.equals(result.getSuccess()) && Boolean.TRUE.equals(result.getModelLoaded());
            
            String message = result == null? "empty model health result" : result.getStatus();

            return buildResult("model-service", healthy, start, message);
        } catch (Exception e) {
            return buildResult("model-service", false, start, e.getMessage());
        }
    }

    private ComponentHealthResult buildResult(String component, boolean healthy, long start, String message) {
        return ComponentHealthResult.builder()
                .component(component)
                .healthy(healthy)
                .costMs(System.currentTimeMillis() - start)
                .message(message)
                .build();
    }
}
