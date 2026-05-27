package com.everypicfound.common.response;
public final class ResultFactory {

    private ResultFactory() {
    }

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
