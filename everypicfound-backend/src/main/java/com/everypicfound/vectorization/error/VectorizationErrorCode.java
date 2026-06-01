package com.everypicfound.vectorization.error;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VectorizationErrorCode implements ErrorCode {
    
    VECTORIZATION_TASK_INVALID(400001, "vectorization task invalid"),

    VECTORIZATION_TASK_PUBLISH_FAILED(400002, "vectorization task publish failed"),

    IMAGE_ASSET_NOT_FOUND(400003, "image asset not found"),

    IMAGE_ASSET_STATUS_INVALID(400004, "image asset status invalid"),

    IMAGE_FILE_NOT_FOUND(400005, "image file not found"),

    IMAGE_FILE_READ_FAILED(400006, "image file read failed"),

    MODEL_VECTORIZATION_FAILED(400007, "model vectorization failed"),

    VECTOR_DIM_MISMATCH(400008, "vector dimension mismatch"),

    VECTOR_UPSERT_FAILED(400009, "vector upsert failed"),

    VECTOR_READY_UPDATE_FAILED(400010, "vector ready status update failed"),

    VECTORIZATION_PROCESS_FAILED(400011, "vectorization process failed");

    private final Integer code;

    private final String message;
}
