package com.everypicfound.search.observability;

/**
 * 单次同步搜索请求的观测上下文。
 *
 * <p>
 * 只保存低基数的指标状态，不保存 queryText、imageId、
 * requestId 等业务数据。
 * </p>
 *
 * <p>
 * 由 SearchApplicationService 在请求开始时初始化，
 * 并在 finally 中清理。
 * </p>
 */
public final class SearchObservationContext {
    
    private static final ThreadLocal<State> CONTEXT = ThreadLocal.withInitial(State::new);

    private SearchObservationContext() {

    }
    
    public static void begin() {
        CONTEXT.set(new State());
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public static void markCacheLookup(CacheLookupStatus status) {
        if (status == null) {
            return;
        }

        State state = CONTEXT.get();
        state.cacheResult = status.getMetricValue();
        state.cacheGetStageResult = status.getStageResult();
    }

    public static void markCacheWrite(
            CacheWriteStatus status) {

        if (status == null) {
            return;
        }

        CONTEXT.get().cachePutStageResult = status.getStageResult();
    }

    public enum CacheLookupStatus {

        HIT("hit", "success"),

        MISS("miss", "success"),

        ERROR("error", "degraded"),

        NOT_APPLICABLE("not_applicable", "skipped");

        private final String metricValue;

        private final String stageResult;

        CacheLookupStatus(
                String metricValue,
                String stageResult) {

            this.metricValue = metricValue;
            this.stageResult = stageResult;
        }

        public String getMetricValue() {
            return metricValue;
        }

        public String getStageResult() {
            return stageResult;
        }
    }

    public enum CacheWriteStatus {

        SUCCESS("success"),

        ERROR("degraded"),

        SKIPPED("skipped");

        private final String stageResult;

        CacheWriteStatus(String stageResult) {
            this.stageResult = stageResult;
        }

        public String getStageResult() {
            return stageResult;
        }
    }

    public static String getCacheResult() {
        return CONTEXT.get().cacheResult;
    }

    public static String getCacheGetStageResult() {
        return CONTEXT.get().cacheGetStageResult;
    }

    public static String getCachePutStageResult() {
        return CONTEXT.get().cachePutStageResult;
    }




    private static final class State {

        private String cacheResult = "not_applicable";

        private String cacheGetStageResult = "skipped";

        private String cachePutStageResult = "skipped";
    }
}
