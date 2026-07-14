import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";

const manifestPath =
    "performance-test/data/image-query-manifest.json";

test("image query manifest defines generated images with hybrid text", () => {
    const manifest = JSON.parse(
        fs.readFileSync(manifestPath, "utf8"),
    );

    assert.ok(Array.isArray(manifest.items));
    assert.ok(manifest.items.length >= 20);
    assert.ok(
        manifest.items.every(
            (item) =>
                typeof item.path === "string"
                && item.path.endsWith(".png"),
        ),
    );
    assert.ok(
        manifest.items.every(
            (item) =>
                typeof item.hybridText === "string"
                && item.hybridText.length > 0,
        ),
    );
});
