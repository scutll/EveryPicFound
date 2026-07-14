import test from "node:test";
import assert from "node:assert/strict";

import {
    buildTextQuery,
    pickTextQuery,
} from "../../scripts/lib/text-queries.js";

const queries = [
    { queryText: "red car", topK: 30 },
    { queryText: "blue sky", topK: 30 },
];

test("pickTextQuery rotates deterministically by vu and iteration", () => {
    assert.deepEqual(
        pickTextQuery(queries, 1, 0),
        queries[1],
    );
    assert.deepEqual(
        pickTextQuery(queries, 2, 0),
        queries[0],
    );
});

test("buildTextQuery returns base query in fixed-pool mode", () => {
    assert.equal(
        buildTextQuery("red car", "fixed-pool", 3, 8),
        "red car",
    );
});

test("buildTextQuery appends stable uniqueness marker in unique mode", () => {
    assert.equal(
        buildTextQuery("red car", "unique", 3, 8, "run-a"),
        "red car run-run-a vu-3 iter-8",
    );
});
