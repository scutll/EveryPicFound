const DATA_ROOT = "performance-test/data";

export function resolveDataFilePath(relativePath) {
    return `${DATA_ROOT}/${relativePath}`;
}

export function getDatasetFileName(relativePath) {
    if (typeof relativePath !== "string" || relativePath.length === 0) {
        throw new Error("Dataset path is required");
    }

    const segments = relativePath.split("/");
    return segments[segments.length - 1];
}

export function pickDatasetItem(dataset, vu, iteration) {
    const items = dataset?.items ?? [];

    if (items.length === 0) {
        throw new Error("Image dataset is empty");
    }

    const normalizedVu = Math.max(Number(vu) || 0, 0);
    const normalizedIteration = Math.max(Number(iteration) || 0, 0);
    const index = (normalizedVu + normalizedIteration) % items.length;

    return items[index];
}

export function buildHybridQueryText(item, queryMode, vu = 0, iteration = 0, runId = "") {
    const baseText = item?.hybridText?.trim();

    if (!baseText) {
        throw new Error("Dataset item hybridText is required");
    }

    if (queryMode === "unique") {
        const runPart = runId ? ` run-${runId}` : "";
        return `${baseText}${runPart} vu-${vu} iter-${iteration}`;
    }

    return baseText;
}
