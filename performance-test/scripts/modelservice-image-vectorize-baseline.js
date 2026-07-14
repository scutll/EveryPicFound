import http from "k6/http";
import { check } from "k6";
import { Rate, Trend } from "k6/metrics";
import { buildScenarioOptions, resolveBaseConfig } from "./lib/config.js";

const config = resolveBaseConfig(__ENV);
const imageBytes = open("../../modelservice/warmup.png", "b");
const imageVectorizeDuration = new Trend("modelservice_image_vectorize_duration", true);
const imageVectorizeFailures = new Rate("modelservice_image_vectorize_failures");
const imageVectorizeSuccesses = new Rate("modelservice_image_vectorize_successes");

export const options = buildScenarioOptions({
    scenarioName: "modelservice_image_vectorize_baseline",
    testType: "modelservice-image-vectorize",
    thresholds: {
        modelservice_image_vectorize_failures: ["rate<0.05"],
    },
});

export default function () {
    const response = http.post(
        `${config.baseUrl}/vectorize/image`,
        {
            traceId: `k6-image-${__VU}`,
            requestId: `k6-image-${__VU}-${__ITER}`,
            imageId: `${100000 + __VU}`,
            file: http.file(imageBytes, "warmup.png", "image/png"),
        },
        {
            tags: {
                endpoint: "modelservice-image-vectorize",
                profile: config.profile,
            },
            timeout: "90s",
        },
    );

    imageVectorizeDuration.add(response.timings.duration);

    const ok = check(response, {
        "image status is 200": (res) => res.status === 200,
        "image vectorization succeeds": (res) => {
            const body = res.json();
            return body.success === true && body.dim === 512 && Array.isArray(body.embedding);
        },
    });

    imageVectorizeSuccesses.add(ok);
    imageVectorizeFailures.add(!ok);
}
