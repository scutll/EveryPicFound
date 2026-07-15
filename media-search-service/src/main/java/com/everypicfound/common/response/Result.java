package com.everypicfound.common.response;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {

    private Integer code;

    private String message;

    private T data;

    private String requestId;

    public static <T> Result<T> success(T data, String requestId) {
        return new Result<>(0, "success", data, requestId);
    }

    public static <T> Result<T> fail(Integer code, String message, String requestId) {
        return new Result<>(code, message, null, requestId);
    }
    
}
