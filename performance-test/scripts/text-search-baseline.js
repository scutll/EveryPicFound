import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";
import { Rate } from "k6/metrics";

import {
    buildScenarioOptions,
    getEnvNumber,
    getEnvString,
    resolveBaseConfig,
} from "./lib/config.js";
import {
    isBusinessSuccess,
    safeJson,
} from "./lib/http.js";
import {
    buildTextQuery,
    pickTextQuery,
} from "./lib/text-queries.js";

const config = resolveBaseConfig();
const thinkTime = getEnvNumber(__ENV, "THINK_TIME", 0);
const queryMode = getEnvString(__ENV, "QUERY_MODE", "fixed-pool");
const runId = getEnvString(__ENV, "RUN_ID", "");

const queries = new SharedArray("text-search-queries", function () {
    return JSON.parse(open("../data/text-queries.json"));
});

const businessFailures = new Rate("business_failures");

export const options = buildScenarioOptions({
    scenarioName: "text_search_baseline",
    testType: "text-search",
    thresholds: {
        http_req_failed: [
            "rate<0.01",
        ],
        checks: [
            "rate>0.99",
        ],
        business_failures: [
            "rate<0.01",
        ],
    },
});

export default function () {
    const query = pickTextQuery(
        queries,
        __VU,
        __ITER,
    );
    const queryText = buildTextQuery(
        query.queryText,
        queryMode,
        __VU,
        __ITER,
        runId,
    );

    const response = http.post(
        `${config.baseUrl}/api/search/text`,
        JSON.stringify({
            queryText,
            topK: query.topK ?? config.topK,
        }),
        {
            headers: {
                "Content-Type": "application/json",
            },
            tags: {
                endpoint: "text-search",
                profile: config.profile,
            },
            timeout: "10s",
        },
    );

    const responseBody = safeJson(response);
    const businessSuccess =
        response.status === 200
        && isBusinessSuccess(responseBody);

    businessFailures.add(!businessSuccess, {
        endpoint: "text-search",
        profile: config.profile,
    });

    check(
        response,
        {
            "HTTP status is 200": (res) =>
                res.status === 200,
            "response is valid JSON": () =>
                responseBody !== null,
            "business code is 0": () =>
                responseBody !== null
                && responseBody.code === 0,
            "response data exists": () =>
                responseBody !== null
                && responseBody.data !== null,
        },
        {
            endpoint: "text-search",
            profile: config.profile,
        },
    );

    if (thinkTime > 0) {
        sleep(thinkTime);
    }
}
