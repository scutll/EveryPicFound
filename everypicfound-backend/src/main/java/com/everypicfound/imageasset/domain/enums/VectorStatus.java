package com.everypicfound.imageasset.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VectorStatus {

    // 待向量化。
    PENDING(1),

    // 向量化处理中。
    PROCESSING(2),

    // 向量已可用。
    READY(3),

    // 向量化失败。
    FAILED(4);

    // 状态码。
    private final Integer code;
}
