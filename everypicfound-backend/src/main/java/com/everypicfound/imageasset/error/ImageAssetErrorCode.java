package com.everypicfound.imageasset.error;

import com.everypicfound.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ImageAssetErrorCode implements ErrorCode {

    // 上传图片为空。
    IMAGE_EMPTY("IMAGE_001", "上传图片为空"),

    // 图片大小超过限制。
    IMAGE_SIZE_EXCEEDED("IMAGE_002", "图片大小超过限制"),

    // 图片格式不支持。
    IMAGE_FORMAT_UNSUPPORTED("IMAGE_003", "图片格式不支持"),

    // MIME 类型不合法。
    IMAGE_MIME_INVALID("IMAGE_004", "MIME 类型不合法"),

    // 图片无法解析。
    IMAGE_DECODE_FAILED("IMAGE_005", "图片无法解析"),

    // 图片已存在。
    DUPLICATE_IMAGE("IMAGE_006", "图片已存在"),

    // 图片不存在。
    IMAGE_NOT_FOUND("IMAGE_007", "图片不存在"),

    // 图片状态不允许当前操作。
    IMAGE_STATUS_INVALID("IMAGE_008", "图片状态不允许当前操作"),

    // 图片元数据入库失败。
    IMAGE_METADATA_SAVE_FAILED("IMAGE_009", "图片元数据入库失败"),

    // 图片状态更新失败。
    IMAGE_STATUS_UPDATE_FAILED("IMAGE_010", "图片状态更新失败"),

    // 向量状态更新失败。
    VECTOR_STATUS_UPDATE_FAILED("IMAGE_011", "向量状态更新失败"),

    // 孤儿文件补偿删除失败。
    ORPHAN_FILE_DELETE_FAILED("IMAGE_012", "孤儿文件补偿删除失败");

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
