import http from "k6/http";
import { check } from "k6";
import {
    Counter,
    Rate,
    Trend,
} from "k6/metrics";

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
import { safeJson } from "./lib/http.js";
import {
    buildImageBatchRequests,
    summarizeReturnedItems,
} from "./lib/search-scenarios.js";

const config = resolveBaseConfig();
const queryMode = getEnvString(__ENV, "QUERY_MODE", "fixed-pool");
const runId = getEnvString(__ENV, "RUN_ID", "");
const manifest = JSON.parse(open("../data/image-query-manifest.json"));
const imageBinaryByPath = loadImageBinaryByPath(manifest);

const pageTotalDuration = new Trend("page_total_duration", true);
const searchDuration = new Trend("page_search_duration", true);
const imageBatchDuration = new Trend("page_image_batch_duration", true);
const imageAccessDuration = new Trend("image_access_duration", true);

const pageFailures = new Rate("page_failures");
const searchFailures = new Rate("search_failures");
const imageFailures = new Rate("image_failures");

const searchIterations = new Counter("search_iterations");
const successfulSearches = new Counter("successful_searches");
const imageBatches = new Counter("image_batches");
const returnedImageItems = new Counter("returned_image_items");
const imageRequests = new Counter("image_requests");
const emptySearchResults = new Counter("empty_search_results");
const invalidImageUrls = new Counter("invalid_image_urls");

export const options = buildScenarioOptions({
    scenarioName: "hybrid_search_image_access",
    testType: "hybrid-search-image-access",
    thresholds: {
        http_req_failed: [
            "rate<0.01",
        ],
        checks: [
            "rate>0.99",
        ],
        page_failures: [
            "rate<0.01",
        ],
        search_failures: [
            "rate<0.01",
        ],
        image_failures: [
            "rate<0.01",
        ],
    },
});

export default function () {
    const pageStart = Date.now();
    const datasetItem = pickDatasetItem(manifest, __VU, __ITER);

    searchIterations.add(1);

    const searchResponse = http.post(
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
                phase: "search",
                profile: config.profile,
            },
            timeout: "10s",
        },
    );

    searchDuration.add(searchResponse.timings.duration, {
        endpoint: "hybrid-search",
        profile: config.profile,
    });

    const responseBody = safeJson(searchResponse);
    const searchSucceeded =
        searchResponse.status === 200
        && responseBody !== null
        && responseBody.code === 0
        && responseBody.data !== null
        && Array.isArray(responseBody.data.items);

    searchFailures.add(!searchSucceeded, {
        endpoint: "hybrid-search",
        profile: config.profile,
    });

    check(
        searchResponse,
        {
            "search HTTP status is 200": (response) =>
                response.status === 200,
            "search response is valid JSON": () =>
                responseBody !== null,
            "search business code is 0": () =>
                responseBody !== null
                && responseBody.code === 0,
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

    if (!searchSucceeded) {
        pageFailures.add(true);
        pageTotalDuration.add(Date.now() - pageStart, {
            profile: config.profile,
        });
        return;
    }

    successfulSearches.add(1);

    const items = responseBody.data.items;
    returnedImageItems.add(items.length);

    if (items.length === 0) {
        emptySearchResults.add(1);
        pageFailures.add(false);
        pageTotalDuration.add(Date.now() - pageStart, {
            profile: config.profile,
        });
        return;
    }

    const summary = summarizeReturnedItems(items);
    invalidImageUrls.add(summary.invalidCount);

    if (summary.validItems.length === 0) {
        pageFailures.add(true);
        pageTotalDuration.add(Date.now() - pageStart, {
            profile: config.profile,
        });
        return;
    }

    const requestDefinitions = buildImageBatchRequests(
        config.baseUrl,
        summary.validItems,
        config.profile,
    );

    imageBatches.add(1);
    imageRequests.add(requestDefinitions.length);

    const imageBatchStart = Date.now();
    const imageResponses = http.batch(requestDefinitions);

    imageBatchDuration.add(Date.now() - imageBatchStart, {
        endpoint: "image-access",
        profile: config.profile,
    });

    let allImagesSucceeded = true;

    for (const imageResponse of imageResponses) {
        const imageSucceeded = imageResponse.status === 200;

        if (!imageSucceeded) {
            allImagesSucceeded = false;
        }

        imageFailures.add(!imageSucceeded, {
            endpoint: "image-access",
            profile: config.profile,
        });

        imageAccessDuration.add(imageResponse.timings.duration, {
            endpoint: "image-access",
            profile: config.profile,
        });

        check(
            imageResponse,
            {
                "image HTTP status is 200": (response) =>
                    response.status === 200,
            },
            {
                endpoint: "image-access",
                profile: config.profile,
            },
        );
    }

    pageFailures.add(
        !allImagesSucceeded || summary.invalidCount > 0,
    );

    pageTotalDuration.add(Date.now() - pageStart, {
        profile: config.profile,
    });
}

function loadImageBinaryByPath(dataset) {
    const imageBinaryMap = {};

    for (const item of dataset.items ?? []) {
        imageBinaryMap[item.path] = open(`../data/${item.path}`, "b");
    }

    return imageBinaryMap;
}
