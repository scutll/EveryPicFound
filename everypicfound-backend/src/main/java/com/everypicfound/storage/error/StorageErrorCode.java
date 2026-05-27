package com.everypicfound.storage.error;

import com.everypicfound.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {

    // 文件保存失败。
    FILE_SAVE_FAILED("STORAGE_001", "文件保存失败"),

    // 文件读取失败。
    FILE_READ_FAILED("STORAGE_002", "文件读取失败"),

    // 文件删除失败。
    FILE_DELETE_FAILED("STORAGE_003", "文件删除失败"),

    // 文件不存在。
    FILE_NOT_FOUND("STORAGE_004", "文件不存在"),

    // 存储不可用。
    STORAGE_UNAVAILABLE("STORAGE_005", "存储不可用"),

    // 存储路径非法。
    STORAGE_PATH_INVALID("STORAGE_006", "存储路径非法");

    // 错误码。
    private final String code;

    // 错误信息。
    private final String message;

    // 获取错误码。
    @Override
    public String getCode() {
        throw new UnsupportedOperationException("TODO");
    }

    // 获取错误信息。
    @Override
    public String getMessage() {
        throw new UnsupportedOperationException("TODO");
    }
}
