export function isBusinessSuccess(responseBody) {
    return responseBody !== null
        && responseBody.code === 0
        && responseBody.data !== null
        && responseBody.data !== undefined;
}

export function safeJson(response) {
    try {
        return response.json();
    } catch (error) {
        return null;
    }
}
