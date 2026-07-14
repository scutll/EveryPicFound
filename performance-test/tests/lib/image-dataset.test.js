import test from "node:test";
import assert from "node:assert/strict";

import {
    pickDatasetItem,
    resolveDataFilePath,
} from "../../scripts/lib/image-dataset.js";

const dataset = {
    items: [
        {
            id: "img-1",
            path: "generated-query-images/query-001.png",
            hybridText: "alpha",
        },
        {
            id: "img-2",
            path: "generated-query-images/query-002.png",
            hybridText: "beta",
        },
    ],
};

test("pickDatasetItem rotates deterministically by vu and iteration", () => {
    assert.deepEqual(
        pickDatasetItem(dataset, 1, 0),
        dataset.items[1],
    );
    assert.deepEqual(
        pickDatasetItem(dataset, 2, 0),
        dataset.items[0],
    );
    assert.deepEqual(
        pickDatasetItem(dataset, 2, 1),
        dataset.items[1],
    );
});

test("resolveDataFilePath keeps dataset paths under performance-test data", () => {
    const fullPath = resolveDataFilePath(
        "generated-query-images/query-001.png",
    );

    assert.match(
        fullPath,
        /performance-test[\\/]data[\\/]generated-query-images[\\/]query-001\.png$/,
    );
});
