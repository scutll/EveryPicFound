import test from "node:test";
import assert from "node:assert/strict";

import {
    buildHybridQueryText,
    pickDatasetItem,
} from "../../scripts/lib/image-dataset.js";

const dataset = {
    items: [
        {
            id: "query-1",
            path: "generated-query-images/query-001.png",
            hybridText: "generated geometric composition 1",
        },
    ],
};

test("buildHybridQueryText returns manifest text in fixed-pool mode", () => {
    const item = pickDatasetItem(dataset, 1, 0);

    assert.equal(
        buildHybridQueryText(item, "fixed-pool"),
        "generated geometric composition 1",
    );
});

test("buildHybridQueryText appends run and iteration in unique mode", () => {
    const item = pickDatasetItem(dataset, 1, 0);

    assert.equal(
        buildHybridQueryText(item, "unique", 2, 7, "run-b"),
        "generated geometric composition 1 run-run-b vu-2 iter-7",
    );
});
