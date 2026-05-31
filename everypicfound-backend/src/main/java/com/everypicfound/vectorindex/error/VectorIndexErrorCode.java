package com.everypicfound.vectorindex.error;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VectorIndexErrorCode implements ErrorCode {
    
    VECTOR_INDEX_UNAVAILABLE(500100, "vector index service unavailable"),
    VECTOR_COLLECTION_NOT_FOUND(500101, "vector collection not found"),
    VECTOR_COLLECTION_CONFIG_INVALID(500102, "vector collection config invalid"),
    VECTOR_DIM_MISMATCH(500103, "vector dimension mismatch"),
    VECTOR_UPSERT_FAILED(500104, "vector upsert failed"),
    VECTOR_SEARCH_FAILED(500105, "vector search failed"),
    VECTOR_DELETE_FAILED(500106, "vector delete failed"),
    VECTOR_EXISTS_CHECK_FAILED(500107, "vector exists check failed");

    private final Integer code;

    private final String message;
}
