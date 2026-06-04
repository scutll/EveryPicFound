package com.everypicfound.search.error;

import com.everypicfound.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchErrorCode implements ErrorCode {

    SEARCH_PARAM_INVALID(500200, "search param invalid"),
    SEARCH_TYPE_INVALID(500201, "search type invalid"),
    SEARCH_IMAGE_EMPTY(500202, "search image is empty"),
    SEARCH_TEXT_EMPTY(500203, "search text is empty"),
    SEARCH_TEXT_TOO_LONG(500204, "search text is too long"),
    SEARCH_TOPK_INVALID(500205, "search topK invalid"),
    SEARCH_COLLECTION_UNAVAILABLE(500206, "search collection unavailable"),
    QUERY_VECTORIZATION_FAILED(500207, "query vectorization failed"),
    QUERY_EMBEDDING_EMPTY(500208, "query embedding is empty"),
    QUERY_VECTOR_DIM_MISMATCH(500209, "query vector dimension mismatch"),
    VECTOR_SEARCH_FAILED(500210, "vector search failed"),
    IMAGE_ASSET_QUERY_FAILED(500211, "image asset query failed"),
    SEARCH_RESULT_EMPTY(500212, "search result is empty");

    private final Integer code;

    private final String message;
}