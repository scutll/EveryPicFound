package com.everypicfound.imageasset.error;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageAssetErrorCode implements ErrorCode{
    IMAGE_EMPTY(200001, "image empty"),

    IMAGE_SIZE_EXCEEDED(200002, "image size excceeded limit"),

    IMAGE_FORMAT_UNSUPPORTED(200003, "image format unsupported"),

    IMAGE_MIME_INVALID(200004, "image mime type invalid"),

    IMAGE_DECODE_FAILED(200005, "failed to decode image"),

    DUPLICATE_IMAGE(200006, "image exists already"),

    IMAGE_METADATA_SAVE_FAILED(200007, "image metadata saving failed"),

    ORPHAN_FILE_DELETE_FAILED(200008, "orphan image deleting failed");

    private final Integer code;

    private final String message;
}
