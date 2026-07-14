const DEFAULT_VUS = 10;
const DEFAULT_DURATION = "3m";
const DEFAULT_PROFILE = "baseline";
const DEFAULT_BASE_URL = "http://127.0.0.1:8080";
const DEFAULT_TOP_K = 30;

export function getEnvString(env, key, fallback) {
    const value = env?.[key];

    if (value === undefined || value === null || value === "") {
        return fallback;
    }

    return String(value);
}

export function getEnvNumber(env, key, fallback) {
    const rawValue = env?.[key];

    if (rawValue === undefined || rawValue === null || rawValue === "") {
        return fallback;
    }

    const parsedValue = Number(rawValue);

    if (!Number.isFinite(parsedValue)) {
        throw new Error(`Invalid numeric env ${key}: ${rawValue}`);
    }

    return parsedValue;
}

export function resolveBaseConfig(env = __ENV) {
    return {
        baseUrl: getEnvString(env, "BASE_URL", DEFAULT_BASE_URL),
        profile: getEnvString(env, "PROFILE", DEFAULT_PROFILE),
        vus: getEnvNumber(env, "VUS", DEFAULT_VUS),
        duration: getEnvString(env, "DURATION", DEFAULT_DURATION),
        topK: getEnvNumber(env, "TOP_K", DEFAULT_TOP_K),
    };
}

export function buildScenarioOptions({
    scenarioName,
    testType,
    env = __ENV,
    thresholds,
}) {
    const config = resolveBaseConfig(env);

    return {
        scenarios: {
            [scenarioName]: {
                executor: "constant-vus",
                vus: config.vus,
                duration: config.duration,
                gracefulStop: "10s",
                tags: {
                    test_type: testType,
                    profile: config.profile,
                },
            },
        },
        summaryTrendStats: [
            "avg",
            "min",
            "med",
            "max",
            "p(90)",
            "p(95)",
            "p(99)",
            "count",
        ],
        summaryTimeUnit: "ms",
        thresholds,
    };
}
