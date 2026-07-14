export function toAbsoluteUrl(imageUrl, baseUrl) {
    if (
        imageUrl.startsWith("http://")
        || imageUrl.startsWith("https://")
    ) {
        return imageUrl;
    }

    if (imageUrl.startsWith("/")) {
        return `${baseUrl}${imageUrl}`;
    }

    return `${baseUrl}/${imageUrl}`;
}

export function buildImageBatchRequests(baseUrl, items, profile) {
    return items.map((item) => ({
        method: "GET",
        url: toAbsoluteUrl(item.imageUrl.trim(), baseUrl),
        params: {
            tags: {
                endpoint: "image-access",
                phase: "image-load",
                profile,
            },
            responseType: "none",
            timeout: "10s",
        },
    }));
}

export function summarizeReturnedItems(items) {
    const validItems = [];
    let invalidCount = 0;

    for (const item of items ?? []) {
        if (
            item === null
            || item === undefined
            || typeof item.imageUrl !== "string"
            || item.imageUrl.trim().length === 0
        ) {
            invalidCount += 1;
            continue;
        }

        validItems.push(item);
    }

    return {
        validItems,
        invalidCount,
    };
}
