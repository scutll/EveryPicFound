import test from "node:test";
import assert from "node:assert/strict";

import { buildScenarioOptions } from "../../scripts/lib/config.js";

test("buildScenarioOptions applies env defaults and tags", () => {
    const options = buildScenarioOptions({
        scenarioName: "image_search_baseline",
        testType: "image-search",
        env: {},
        thresholds: {
            http_req_failed: ["rate<0.01"],
        },
    });

    assert.equal(
        options.scenarios.image_search_baseline.executor,
        "constant-vus",
    );
    assert.equal(
        options.scenarios.image_search_baseline.vus,
        10,
    );
    assert.equal(
        options.scenarios.image_search_baseline.duration,
        "3m",
    );
    assert.equal(
        options.scenarios.image_search_baseline.tags.test_type,
        "image-search",
    );
    assert.equal(
        options.scenarios.image_search_baseline.tags.profile,
        "baseline",
    );
});
