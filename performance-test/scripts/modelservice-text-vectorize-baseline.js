import http from "k6/http";
import { check } from "k6";
import { Rate, Trend } from "k6/metrics";
import { buildScenarioOptions, resolveBaseConfig } from "./lib/config.js";

const config = resolveBaseConfig(__ENV);
const textVectorizeDuration = new Trend("modelservice_text_vectorize_duration", true);
const textVectorizeFailures = new Rate("modelservice_text_vectorize_failures");
const textVectorizeSuccesses = new Rate("modelservice_text_vectorize_successes");

const queries = [
    "a girl sitting by a window in a warm cafe",
    "a red sports car on a city street",
    "a mountain lake under a blue sky",
    "a small dog running on grass",
    "a bowl of ramen on a wooden table",
    "a modern office desk with a laptop",
    "a beach sunset with orange clouds",
    "a black cat sleeping on a sofa",
];

export const options = buildScenarioOptions({
    scenarioName: "modelservice_text_vectorize_baseline",
    testType: "modelservice-text-vectorize",
    thresholds: {
        modelservice_text_vectorize_failures: ["rate<0.05"],
    },
});

export default function () {
    const query = queries[(__VU + __ITER) % queries.length];
    const response = http.post(
        `${config.baseUrl}/vectorize/text`,
        {
            traceId: `k6-text-${__VU}`,
            requestId: `k6-text-${__VU}-${__ITER}`,
            text: query,
        },
        {
            tags: {
                endpoint: "modelservice-text-vectorize",
                profile: config.profile,
            },
            timeout: "60s",
        },
    );

    textVectorizeDuration.add(response.timings.duration);

    const ok = check(response, {
        "text status is 200": (res) => res.status === 200,
        "text vectorization succeeds": (res) => {
            const body = res.json();
            return body.success === true && body.dim === 512 && Array.isArray(body.embedding);
        },
    });

    textVectorizeSuccesses.add(ok);
    textVectorizeFailures.add(!ok);
}
