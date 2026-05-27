package com.everypicfound.imageasset.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageStatus {

    // 正常图片资产。
    NORMAL(1),

    // 已删除图片资产。
    DELETED(2),

    // 异常图片资产。
    INVALID(3);

    // 状态码。
    private final Integer code;
}
