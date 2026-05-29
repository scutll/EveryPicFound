package com.everypicfound.storage.error;

import com.everypicfound.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum StorageErrorCode implements ErrorCode {

    FILE_SAVE_FAILED(300001, "file save failed"),

    FILE_DELETE_FAILED(300002, "file delete failed"),

    FILE_NOT_FOUND(300003, "file not found"),

    STORAGE_UNAVAILABLE(300004, "storage unavailable"),

    STORAGE_PATH_INVALID(300005, "file sotrage path invalid"),
                
    FILE_READ_FAILED(300006, "file read failed");

    private final Integer code;

    private final String message;
}