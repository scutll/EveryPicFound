import http from "k6/http";
import { check } from "k6";
import {
    Counter,
    Rate,
    Trend,
} from "k6/metrics";

import {
    buildScenarioOptions,
    getEnvNumber,
    getEnvString,
    resolveBaseConfig,
} from "./lib/config.js";
import { safeJson } from "./lib/http.js";
import {
    buildTextQuery,
    pickTextQuery,
} from "./lib/text-queries.js";
import { SharedArray } from "k6/data";
import { buildImageBatchRequests } from "./lib/search-scenarios.js";

const config = resolveBaseConfig();
const queryMinWords = getEnvNumber(__ENV, "QUERY_MIN_WORDS", 5);
const queryMaxWords = getEnvNumber(__ENV, "QUERY_MAX_WORDS", 21);
const queryMode = getEnvString(__ENV, "QUERY_MODE", "unique");
const runId = getEnvString(__ENV, "RUN_ID", "");

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

const queryTemplates = new SharedArray("text-search-query-templates", function () {
    return JSON.parse(open("../data/text-queries.json"));
});

export const options = buildScenarioOptions({
    scenarioName: "text_search_image_access",
    testType: "text-search-image-access",
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
    const queryText = createScenarioQuery();

    searchIterations.add(1);

    const searchResponse = http.post(
        `${config.baseUrl}/api/search/text`,
        JSON.stringify({
            queryText,
            topK: config.topK,
        }),
        {
            headers: {
                "Content-Type": "application/json",
            },
            tags: {
                endpoint: "text-search",
                phase: "search",
                profile: config.profile,
            },
            timeout: "10s",
        },
    );

    searchDuration.add(
        searchResponse.timings.duration,
        {
            endpoint: "text-search",
            profile: config.profile,
        },
    );

    const responseBody = safeJson(searchResponse);
    const searchSucceeded =
        searchResponse.status === 200
        && responseBody !== null
        && responseBody.code === 0
        && responseBody.data !== null
        && Array.isArray(responseBody.data.items);

    searchFailures.add(!searchSucceeded, {
        endpoint: "text-search",
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
            endpoint: "text-search",
            profile: config.profile,
        },
    );

    if (!searchSucceeded) {
        pageFailures.add(true);
        pageTotalDuration.add(
            Date.now() - pageStart,
            {
                profile: config.profile,
            },
        );
        return;
    }

    successfulSearches.add(1);

    const items = responseBody.data.items;
    returnedImageItems.add(items.length);

    if (items.length === 0) {
        emptySearchResults.add(1);
        pageFailures.add(false);
        pageTotalDuration.add(
            Date.now() - pageStart,
            {
                profile: config.profile,
            },
        );
        return;
    }

    const validItems = [];

    for (const item of items) {
        if (
            item === null
            || item === undefined
            || typeof item.imageUrl !== "string"
            || item.imageUrl.trim().length === 0
        ) {
            invalidImageUrls.add(1);
            continue;
        }

        validItems.push(item);
    }

    const allUrlsValid = validItems.length === items.length;

    if (!allUrlsValid) {
        pageFailures.add(true);
    }

    if (validItems.length === 0) {
        pageTotalDuration.add(
            Date.now() - pageStart,
            {
                profile: config.profile,
            },
        );
        return;
    }

    const requestDefinitions = buildImageBatchRequests(
        config.baseUrl,
        validItems,
        config.profile,
    );

    imageBatches.add(1);
    imageRequests.add(requestDefinitions.length);

    const imageBatchStart = Date.now();
    const imageResponses = http.batch(requestDefinitions);

    imageBatchDuration.add(
        Date.now() - imageBatchStart,
        {
            endpoint: "image-access",
            profile: config.profile,
        },
    );

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

        imageAccessDuration.add(
            imageResponse.timings.duration,
            {
                endpoint: "image-access",
                profile: config.profile,
            },
        );

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
        !allImagesSucceeded || !allUrlsValid,
    );

    pageTotalDuration.add(
        Date.now() - pageStart,
        {
            profile: config.profile,
        },
    );
}

const adjectives = [
    "small",
    "large",
    "bright",
    "dark",
    "quiet",
    "busy",
    "modern",
    "ancient",
    "beautiful",
    "colorful",
    "green",
    "snowy",
    "sunny",
    "golden",
    "red",
    "blue",
];

const subjects = [
    "cat",
    "dog",
    "bird",
    "person",
    "car",
    "bicycle",
    "building",
    "mountain",
    "river",
    "forest",
    "street",
    "lake",
    "city",
    "garden",
    "beach",
    "food",
];

const actions = [
    "running",
    "walking",
    "standing",
    "sitting",
    "driving",
    "flying",
    "resting",
    "playing",
    "moving",
    "looking",
];

const relations = [
    "near",
    "beside",
    "under",
    "above",
    "behind",
    "inside",
    "around",
    "in front of",
    "next to",
];

const scenes = [
    "at sunset",
    "in the morning",
    "at night",
    "under blue sky",
    "during winter",
    "during summer",
    "in the rain",
    "on the road",
    "by the water",
    "in the distance",
];

const punctuation = [
    "",
    ".",
    "?",
    "!",
    ",",
];

const templateBuilders = [
    () =>
        `${randomValue(adjectives)} `
        + `${randomValue(subjects)} `
        + `${randomValue(relations)} `
        + `${randomValue(adjectives)} `
        + `${randomValue(subjects)}`,
    () =>
        `${randomValue(subjects)} `
        + `${randomValue(actions)} `
        + `${randomValue(scenes)}`,
    () =>
        `${randomValue(adjectives)} `
        + `${randomValue(subjects)} `
        + `${randomValue(scenes)}`,
    () =>
        `${randomValue(subjects)} `
        + `${randomValue(relations)} `
        + `${randomValue(subjects)} `
        + `${randomValue(scenes)}`,
];

function createScenarioQuery() {
    const seedQuery = pickTextQuery(
        queryTemplates,
        __VU,
        __ITER,
    );

    if (queryMode === "fixed-pool") {
        return buildTextQuery(
            seedQuery.queryText,
            "fixed-pool",
            __VU,
            __ITER,
            runId,
        );
    }

    return createUniqueQuery(seedQuery);
}

function createUniqueQuery(seedQuery) {
    const targetWords = randomInteger(
        queryMinWords,
        queryMaxWords,
    );

    let query =
        seedQuery?.queryText?.trim()
        || randomValue(templateBuilders)();

    while (countWords(query) < targetWords) {
        query +=
            ` ${randomValue(relations)} `
            + `${randomValue(adjectives)} `
            + `${randomValue(subjects)}`;
    }

    const words = query.trim().split(/\s+/);
    query = words.slice(0, targetWords).join(" ");

    return buildTextQuery(
        query + randomValue(punctuation),
        "unique",
        __VU,
        __ITER,
        runId,
    );
}

function randomInteger(minimum, maximum) {
    return Math.floor(
        Math.random() * (maximum - minimum + 1),
    ) + minimum;
}

function randomValue(values) {
    return values[
        Math.floor(Math.random() * values.length)
    ];
}

function countWords(value) {
    return value
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .length;
}
