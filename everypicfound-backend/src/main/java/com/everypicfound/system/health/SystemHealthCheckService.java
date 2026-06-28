package com.everypicfound.system.health;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sql.DataSource;
import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ModelHealthResult;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.collection.health.VectorCollectionHealthChecker;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 系统健康检查服务。
 *
 * <p>
 * 负责检查 MySQL、Qdrant 和模型服务，并统一完成：
 * </p>
 *
 * <ul>
 * <li>整体健康检查指标；</li>
 * <li>单组件健康检查指标；</li>
 * <li>组件当前状态 Gauge；</li>
 * <li>组件 DOWN 和恢复事件；</li>
 * <li>最新健康快照维护。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SystemHealthCheckService {

    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(
            SystemHealthCheckService.class);

    private static final String MODULE = "system-health";

    private static final String BIZ_TYPE = "SYSTEM_HEALTH";

    private static final String OPERATION_CHECK_COMPONENT = "check-component";

    private static final String RESULT_UP = "up";

    private static final String RESULT_DOWN = "down";

    private static final String RESULT_FAILED = "failed";

    private static final int DATABASE_VALID_TIMEOUT_SECONDS = 3;

    private static final int MAX_LOG_MESSAGE_LENGTH = 512;

    private final DataSource dataSource;

    private final ModelVectorizationClient modelVectorizationClient;

    private final VectorCollectionHealthChecker vectorCollectionHealthChecker;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    private final HealthComponentStatusGaugeBinder statusGaugeBinder;

    private final AtomicReference<SystemHealthSnapshot> latestSnapshot = new AtomicReference<>();

    /**
     * 保存组件上一次检查状态。
     *
     * <p>
     * Boolean.TRUE 表示上一次为 UP；
     * Boolean.FALSE 表示上一次为 DOWN 或 FAILED。
     * </p>
     */
    private final Map<HealthComponent, Boolean> previousComponentStates = new ConcurrentHashMap<>();

    /**
     * 兼容原有调用方式。
     */
    public SystemHealthSnapshot check(
            boolean ensureVectorCollection) {

        return check(
                ensureVectorCollection,
                HealthCheckSource.ON_DEMAND);
    }

    /**
     * 执行完整健康检查。
     *
     * @param ensureVectorCollection 是否确保 active collection 可用
     * @param source                 检查来源
     * @return 系统健康快照
     */
    public SystemHealthSnapshot check(boolean ensureVectorCollection, HealthCheckSource source) {
        long startNanos = System.nanoTime();

        HealthCheckSource actualSource = source == null ? HealthCheckSource.ON_DEMAND : source;

        String overallResult = RESULT_FAILED;

        try{
            ComponentCheckOutcome database = checkDatabase();
    
            ComponentCheckOutcome vectorIndex = checkVectorIndex(ensureVectorCollection);
    
            ComponentCheckOutcome modelService = checkModelService();
    
            processComponentState(database);
            processComponentState(vectorIndex);
            processComponentState(modelService);
    
            List<ComponentHealthResult> components = List.of(database.getHealthResult(), vectorIndex.getHealthResult(), modelService.getHealthResult());
    
            boolean healthy = components.stream()
                    .allMatch(item -> Boolean.TRUE.equals(item.getHealthy()));
    
            overallResult = resolveOverallResult(healthy, database, vectorIndex, modelService);
    
    
            SystemHealthSnapshot snapshot = SystemHealthSnapshot.builder()
                    .healthy(healthy)
                    .checkTime(LocalDateTime.now())
                    .components(components)
                    .build();
    
            latestSnapshot.set(snapshot);
    
            return snapshot;
            
        } finally {
            recordOverallMetrics(actualSource, overallResult, startNanos);
        }
        
    }

    /**
     * 获取最近一次健康检查结果。
     *
     * <p>
     * 尚未执行过检查时，主动触发一次非创建型检查。
     * </p>
     */
    public SystemHealthSnapshot getLatestSnapshot() {
        SystemHealthSnapshot snapshot = latestSnapshot.get();
        if (snapshot != null) {
            return snapshot;
        }
        return check(false, HealthCheckSource.ON_DEMAND);
    }

    /**
     * 检查 MySQL。
     */
    private ComponentCheckOutcome checkDatabase() {
        long startNanos = System.nanoTime();

        try (Connection connection = dataSource.getConnection()) {
            boolean valid = connection.isValid(DATABASE_VALID_TIMEOUT_SECONDS);
            if (!valid) {
                return completeComponentCheck(
                        HealthComponent.MYSQL,
                        false,
                        RESULT_DOWN,
                        "database connection is invalid",
                        null,
                        startNanos);
            }

            return completeComponentCheck(
                    HealthComponent.MYSQL,
                    true,
                    RESULT_UP,
                    "UP",
                    null,
                    startNanos);

        } catch (Exception exception) {
            return completeComponentCheck(
                    HealthComponent.MYSQL,
                    false,
                    RESULT_FAILED,
                    resolveThrowableMessage(exception),
                    exception,
                    startNanos);
        }
    }

    /**
     * 检查 Qdrant 及 active collection。
     */
    private ComponentCheckOutcome checkVectorIndex(boolean ensureVectorCollection) {
        long startNanos = System.nanoTime();

        try {
            if (ensureVectorCollection) {
                /*
                 * 启动阶段允许检查并创建缺失的 active collection。
                 */
                vectorCollectionHealthChecker.ensureActiveCollectionReady();

                return completeComponentCheck(
                        HealthComponent.QDRANT,
                        true,
                        RESULT_UP,
                        "UP",
                        null,
                        startNanos);
            } else {
                vectorCollectionHealthChecker.check();

                VectorCollectionConfig config = activeCollectionResolver.resolveActiveCollection();

                if (config == null
                        || config.getCollectionName() == null
                        || config.getCollectionName().isBlank()) {

                    return completeComponentCheck(
                            HealthComponent.QDRANT,
                            false,
                            RESULT_FAILED,
                            "active collection config is invalid",
                            null,
                            startNanos);
                }

                boolean exists = vectorCollectionHealthChecker.checkCollectionExists(config.getCollectionName());

                if (!exists) {
                    return completeComponentCheck(
                            HealthComponent.QDRANT,
                            false,
                            RESULT_DOWN,
                            "active collection does not exist",
                            null,
                            startNanos);
                }

                return completeComponentCheck(
                        HealthComponent.QDRANT,
                        true,
                        RESULT_UP,
                        "UP",
                        null,
                        startNanos);

            }

        } catch (Exception exception) {
            return completeComponentCheck(
                    HealthComponent.QDRANT,
                    false,
                    RESULT_FAILED,
                    resolveThrowableMessage(exception),
                    exception,
                    startNanos);

        }
    }


    /**
     * 检查 Python 模型服务。
     */
    private ComponentCheckOutcome checkModelService() {
        long startNanos = System.nanoTime();

        try {
            ModelHealthResult result = modelVectorizationClient.checkHealth();

            if (result == null) {
                return completeComponentCheck(
                        HealthComponent.MODEL_SERVICE,
                        false,
                        RESULT_FAILED,
                        "empty model health result",
                        null,
                        startNanos);
            }
            boolean healthy = Boolean.TRUE.equals(
                    result.getSuccess())
                    && Boolean.TRUE.equals(
                            result.getModelLoaded());

            if (!healthy) {
                return completeComponentCheck(
                        HealthComponent.MODEL_SERVICE,
                        false,
                        RESULT_DOWN,
                        resolveModelHealthMessage(result),
                        null,
                        startNanos);
            }

            return completeComponentCheck(
                    HealthComponent.MODEL_SERVICE,
                    true,
                    RESULT_UP,
                    resolveModelHealthMessage(result),
                    null,
                    startNanos);
            
        } catch (Exception exception) {
            return completeComponentCheck(
                    HealthComponent.MODEL_SERVICE,
                    false,
                    RESULT_FAILED,
                    resolveThrowableMessage(exception),
                    exception,
                    startNanos);
        }
    }


    /**
     * 完成组件检查并记录组件指标。
     */
    private ComponentCheckOutcome completeComponentCheck(
            HealthComponent component,
            boolean healthy,
            String result,
            String message,
            Throwable throwable,
            long startNanos) {

        long costMs = elapsedMillis(startNanos);

        ComponentHealthResult healthResult = ComponentHealthResult.builder()
                .component(component.getValue())
                .healthy(healthy)
                .costMs(costMs)
                .message(safeMessage(message))
                .build();

        recordComponentMetrics(
                component,
                result,
                costMs);

        return new ComponentCheckOutcome(
                component,
                result,
                healthResult,
                throwable);
    }

    /**
     * 更新 Gauge，并处理组件状态迁移。
     *
     * <p>
     * 初次检查直接为 DOWN 时，也记录一次 DOWN，
     * 但后续持续 DOWN 不重复刷日志。
     * </p>
     */
    private synchronized void processComponentState(ComponentCheckOutcome outcome) {
        if (outcome == null || outcome.getComponent() == null || outcome.getHealthResult() == null) {
            return;
        }

        HealthComponent component = outcome.getComponent();

        boolean healthy = Boolean.TRUE.equals(outcome.getHealthResult().getHealthy());

        statusGaugeBinder.updateStatus(component, healthy);

        Boolean previousHealthy = previousComponentStates.put(component, healthy);

        /*
         * UNKNOWN -> DOWN 或 UP -> DOWN。
         * put function returns previous value or null
         */
        if (!healthy
                && (previousHealthy == null
                        || Boolean.TRUE.equals(
                                previousHealthy))) {

            recordComponentDown(outcome);
            return;
        }

        /*
         * DOWN -> UP。
         */
        if(healthy 
                && Boolean.FALSE.equals(previousHealthy)) {
                recordComponentRecovered(outcome);
        }

        
    }



    /**
     * 组件首次不可用或由 UP 转为 DOWN。
     */
    private void recordComponentDown(
            ComponentCheckOutcome outcome) {

        SystemHealthErrorCode errorCode = resolveComponentErrorCode(
                outcome.getComponent());

        LogContext errorContext = LogContext.builder()
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(
                        OPERATION_CHECK_COMPONENT)
                .eventName(
                        LogEventName.SYSTEM_COMPONENT_DOWN)
                .status(LogStatus.FAILED)
                .costMs(
                        outcome.getHealthResult()
                                .getCostMs())
                .errorCode(String.valueOf(
                        errorCode.getCode()))
                .message(buildDownMessage(
                        outcome))
                .build();

        recordErrorSafely(
                errorContext,
                outcome.getThrowable());

        LogContext eventContext = errorContext.toBuilder()
                .eventName(
                        LogEventName.SYSTEM_COMPONENT_DOWN)
                .build();

        recordEventSafely(eventContext);
    }

    /**
     * 组件由 DOWN 恢复为 UP。
     */
    private void recordComponentRecovered(
            ComponentCheckOutcome outcome) {

        LogContext context = LogContext.builder()
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(
                        OPERATION_CHECK_COMPONENT)
                .eventName(
                        LogEventName.SYSTEM_COMPONENT_RECOVERED)
                /*
                 * 当前 LogStatus 没有 RECOVERED，
                 * 因此恢复事件不强行填入错误状态。
                 */
                .status(LogStatus.RECOVERED)
                .costMs(
                        outcome.getHealthResult()
                                .getCostMs())
                .message(
                        "system component recovered"
                                + ", component="
                                + outcome.getComponent()
                                        .getValue())
                .build();

        recordEventSafely(context);
    }

    private void recordOverallMetrics(
            HealthCheckSource source,
            String result,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(
                        MetricTag.SOURCE,
                        source.getValue())
                .tag(
                        MetricTag.RESULT,
                        result)
                .build();

        metricRecorder.increment(
                MetricName.HEALTH_CHECKS,
                tags);

        metricRecorder.recordTimer(
                MetricName.HEALTH_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private void recordComponentMetrics(
            HealthComponent component,
            String result,
            long costMs) {

        MetricTags tags = MetricTags.builder()
                .tag(
                        MetricTag.COMPONENT,
                        component.getValue())
                .tag(
                        MetricTag.RESULT,
                        result)
                .build();

        metricRecorder.increment(
                MetricName.HEALTH_COMPONENT_CHECKS,
                tags);

        metricRecorder.recordTimer(
                MetricName.HEALTH_COMPONENT_DURATION,
                costMs,
                tags);
    }

    /**
     * 整体结果规则：
     *
     * <ul>
     * <li>存在检查执行异常：failed；</li>
     * <li>检查正常执行但组件不可用：down；</li>
     * <li>全部组件可用：up。</li>
     * </ul>
     */
    private String resolveOverallResult(
            boolean healthy,
            ComponentCheckOutcome... outcomes) {

        if (outcomes != null) {
            for (ComponentCheckOutcome outcome : outcomes) {

                if (outcome != null
                        && RESULT_FAILED.equals(
                                outcome.getResult())) {

                    return RESULT_FAILED;
                }
            }
        }

        return healthy
                ? RESULT_UP
                : RESULT_DOWN;
    }

    private String buildDownMessage(
            ComponentCheckOutcome outcome) {

        return safeMessage(
                "system component unavailable"
                        + ", component="
                        + outcome.getComponent()
                                .getValue()
                        + ", result="
                        + outcome.getResult()
                        + ", detail="
                        + outcome.getHealthResult()
                                .getMessage());
    }

    private SystemHealthErrorCode resolveComponentErrorCode(
            HealthComponent component) {

        if (component == null) {
            return SystemHealthErrorCode.HEALTH_CHECK_EXECUTION_FAILED;
        }

        return switch (component) {
            case MYSQL ->
                SystemHealthErrorCode.MYSQL_UNHEALTHY;
            case QDRANT ->
                SystemHealthErrorCode.QDRANT_UNHEALTHY;
            case MODEL_SERVICE ->
                SystemHealthErrorCode.MODEL_SERVICE_UNHEALTHY;
        };
    }

    private String resolveModelHealthMessage(
            ModelHealthResult result) {

        if (result == null) {
            return "empty model health result";
        }

        if (result.getMessage() != null
                && !result.getMessage().isBlank()) {

            return result.getMessage();
        }

        if (result.getStatus() != null
                && !result.getStatus().isBlank()) {

            return result.getStatus();
        }

        return Boolean.TRUE.equals(result.getSuccess())
                ? "UP"
                : "DOWN";
    }

    private String resolveThrowableMessage(
            Throwable throwable) {

        if (throwable == null) {
            return "health check failed";
        }

        if (throwable.getMessage() == null
                || throwable.getMessage().isBlank()) {

            return throwable.getClass().getName();
        }

        return throwable.getMessage();
    }

    private String safeMessage(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value
                .replace('\r', ' ')
                .replace('\n', ' ');

        if (normalized.length() <= MAX_LOG_MESSAGE_LENGTH) {

            return normalized;
        }

        return normalized.substring(
                0,
                MAX_LOG_MESSAGE_LENGTH);
    }

    private long elapsedMillis(
            long startNanos) {

        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

    private void recordErrorSafely(
            LogContext context,
            Throwable throwable) {

        try {
            if (throwable == null) {
                logService.recordError(context);
            } else {
                logService.recordError(
                        context,
                        throwable);
            }
        } catch (RuntimeException loggingException) {
            if (throwable != null) {
                FALLBACK_LOGGER.error(
                        "System component health check failed",
                        throwable);
            } else {
                FALLBACK_LOGGER.error(
                        "System component health check failed, context={}",
                        context);
            }

            FALLBACK_LOGGER.error(
                    "Failed to record health check error",
                    loggingException);
        }
    }

    private void recordEventSafely(
            LogContext context) {

        try {
            logService.recordEvent(context);
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Failed to record health state event, eventName={}",
                    context == null
                            ? null
                            : context.getEventName(),
                    loggingException);
        }
    }

    /**
     * 单次组件检查的内部结果。
     *
     * <p>
     * Throwable 不进入对外健康快照，
     * 只用于状态首次转 DOWN 时记录完整异常。
     * </p>
     */
    private static final class ComponentCheckOutcome {
        private final HealthComponent component;

        private final String result;

        private final ComponentHealthResult healthResult;

        private final Throwable throwable;

        private ComponentCheckOutcome(
                HealthComponent component,
                String result,
                ComponentHealthResult healthResult,
                Throwable throwable) {

            this.component = component;
            this.result = result;
            this.healthResult = healthResult;
            this.throwable = throwable;
        }

        HealthComponent getComponent() {
            return component;
        }

        String getResult() {
            return result;
        }

        ComponentHealthResult getHealthResult() {
            return healthResult;
        }

        Throwable getThrowable() {
            return throwable;
        }
    }
}
