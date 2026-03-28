package com.globalvibe.arbitrage.common.api;

public record ApiResponse<T>(
        boolean success,
        T data,
        String errorCode,
        String message
) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> failure(String errorCode, String message) {
        return new ApiResponse<>(false, null, errorCode, message);
    }
}
