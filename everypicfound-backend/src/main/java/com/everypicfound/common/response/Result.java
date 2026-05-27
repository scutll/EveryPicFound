package com.everypicfound.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    // 响应状态码。
    private String code;

    // 响应消息。
    private String message;

    // 响应数据。
    private T data;

    // 请求 ID，用于问题排查和链路追踪。
    private String requestId;

    // 创建成功返回结果。
    public static <T> Result<T> success(T data) {
        throw new UnsupportedOperationException("TODO");
    }

    // 创建成功返回结果。
    public static <T> Result<T> success(T data, String requestId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 创建失败返回结果。
    public static <T> Result<T> fail(String code, String message) {
        throw new UnsupportedOperationException("TODO");
    }

    // 创建失败返回结果。
    public static <T> Result<T> fail(String code, String message, String requestId) {
        throw new UnsupportedOperationException("TODO");
    }
}
