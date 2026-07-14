import http from "k6/http";
import { check } from "k6";
import { Rate } from "k6/metrics";

import {
    buildScenarioOptions,
    getEnvString,
    resolveBaseConfig,
} from "./lib/config.js";
import {
    buildHybridQueryText,
    getDatasetFileName,
    pickDatasetItem,
} from "./lib/image-dataset.js";
import {
    isBusinessSuccess,
    safeJson,
} from "./lib/http.js";

const config = resolveBaseConfig();
const queryMode = getEnvString(__ENV, "QUERY_MODE", "fixed-pool");
const runId = getEnvString(__ENV, "RUN_ID", "");
const manifest = JSON.parse(open("../data/image-query-manifest.json"));
const imageBinaryByPath = loadImageBinaryByPath(manifest);

const businessFailures = new Rate("business_failures");

export const options = buildScenarioOptions({
    scenarioName: "hybrid_search_baseline",
    testType: "hybrid-search",
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
    const datasetItem = pickDatasetItem(
        manifest,
        __VU,
        __ITER,
    );

    const response = http.post(
        `${config.baseUrl}/api/search/hybrid`,
        {
            queryImage: http.file(
                imageBinaryByPath[datasetItem.path],
                getDatasetFileName(datasetItem.path),
                "image/png",
            ),
            queryText: buildHybridQueryText(
                datasetItem,
                queryMode,
                __VU,
                __ITER,
                runId,
            ),
            topK: String(config.topK),
        },
        {
            tags: {
                endpoint: "hybrid-search",
                profile: config.profile,
            },
            timeout: "10s",
        },
    );

    const responseBody = safeJson(response);
    const businessSuccess =
        response.status === 200
        && isBusinessSuccess(responseBody)
        && Array.isArray(responseBody.data.items);

    businessFailures.add(!businessSuccess, {
        endpoint: "hybrid-search",
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
            "search items exist": () =>
                responseBody !== null
                && responseBody.data !== null
                && Array.isArray(responseBody.data.items),
        },
        {
            endpoint: "hybrid-search",
            profile: config.profile,
        },
    );
}

function loadImageBinaryByPath(dataset) {
    const imageBinaryMap = {};

    for (const item of dataset.items ?? []) {
        imageBinaryMap[item.path] = open(`../data/${item.path}`, "b");
    }

    return imageBinaryMap;
}
