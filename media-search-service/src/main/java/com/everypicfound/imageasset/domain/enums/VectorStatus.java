package com.everypicfound.imageasset.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VectorStatus {
    PENDING(1, "pending for vectorization"),

    PROCESSING(2, "vectorizing"),

    READY(3, "vector available"),

    FAILED(4, "vectorization failed");

    private final Integer code;

    private final String description;
}