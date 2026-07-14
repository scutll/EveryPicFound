import test from "node:test";
import assert from "node:assert/strict";

import {
    buildImageBatchRequests,
    summarizeReturnedItems,
    toAbsoluteUrl,
} from "../../scripts/lib/search-scenarios.js";

test("buildImageBatchRequests creates one GET per returned item", () => {
    const requests = buildImageBatchRequests(
        "http://127.0.0.1:8080",
        [
            { imageUrl: "/images/a.png" },
            { imageUrl: "images/b.png" },
        ],
        "baseline",
    );

    assert.equal(requests.length, 2);
    assert.equal(
        requests[0].url,
        "http://127.0.0.1:8080/images/a.png",
    );
    assert.equal(
        requests[1].url,
        "http://127.0.0.1:8080/images/b.png",
    );
});

test("toAbsoluteUrl keeps absolute URLs unchanged", () => {
    assert.equal(
        toAbsoluteUrl(
            "http://example.com/a.png",
            "http://127.0.0.1:8080",
        ),
        "http://example.com/a.png",
    );
});

test("summarizeReturnedItems counts valid and invalid image URLs separately", () => {
    const summary = summarizeReturnedItems([
        { imageUrl: "/images/a.png" },
        { imageUrl: "" },
        {},
    ]);

    assert.equal(summary.validItems.length, 1);
    assert.equal(summary.invalidCount, 2);
});
