package com.everypicfound.imageasset.domain.enums;

public enum FailReason {

    // 图片文件不存在。
    FILE_NOT_FOUND,

    // 图片解码失败。
    IMAGE_DECODE_ERROR,

    // 模型服务超时。
    MODEL_SERVICE_TIMEOUT,

    // 模型服务异常。
    MODEL_SERVICE_ERROR,

    // 向量维度不匹配。
    VECTOR_DIM_MISMATCH,

    // 向量库写入失败。
    VECTOR_DB_UPSERT_FAILED,

    // READY 状态更新失败。
    READY_UPDATE_FAILED,

    // 超过最大重试次数。
    RETRY_LIMIT_EXCEEDED
}
