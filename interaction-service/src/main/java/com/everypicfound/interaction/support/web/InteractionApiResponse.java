package com.everypicfound.interaction.support.web;

public record InteractionApiResponse<T>(
        int code,
        String message,
        T data,
        String requestId) {

    public static <T> InteractionApiResponse<T> success(
            T data,
            String requestId) {
        return new InteractionApiResponse<>(
                0,
                "success",
                data,
                requestId);
    }

    public static InteractionApiResponse<Void> failure(
            int code,
            String message,
            String requestId) {
        return new InteractionApiResponse<>(
                code,
                message,
                null,
                requestId);
    }
}
