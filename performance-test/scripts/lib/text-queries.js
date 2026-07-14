const TEXT_QUERY_DATA_PATH = "../data/text-queries.json";

export function textQueryDataPath() {
    return TEXT_QUERY_DATA_PATH;
}

export function pickTextQuery(queries, vu, iteration) {
    if (!Array.isArray(queries) || queries.length === 0) {
        throw new Error("Text query dataset is empty");
    }

    return queries[(vu + iteration) % queries.length];
}

export function buildTextQuery(queryText, queryMode, vu, iteration, runId = "") {
    const baseText = String(queryText ?? "").trim();

    if (!baseText) {
        throw new Error("Base text query is required");
    }

    if (queryMode === "unique") {
        const runPart = runId ? ` run-${runId}` : "";
        return `${baseText}${runPart} vu-${vu} iter-${iteration}`;
    }

    return baseText;
}
