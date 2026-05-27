package com.everypicfound.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    // 参数错误。
    PARAM_INVALID("COMMON_400", "参数错误"),

    // 系统错误。
    SYSTEM_ERROR("COMMON_500", "系统错误"),

    // 请求过多。
    TOO_MANY_REQUESTS("COMMON_429", "请求过多");

    // 响应状态码。
    private final String code;

    // 响应消息。
    private final String message;
}
