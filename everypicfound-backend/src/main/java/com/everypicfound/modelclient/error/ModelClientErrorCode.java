package com.everypicfound.modelclient.error;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ModelClientErrorCode implements ErrorCode{
    /**
     * 模型服务超时。
     */
    MODEL_SERVICE_TIMEOUT(500001, "model service timeout"),

    /**
     * 模型服务不可用。
     */
    MODEL_SERVICE_UNAVAILABLE(500002, "model service unavailable"),

    /**
     * 模型服务异常。
     */
    MODEL_SERVICE_ERROR(500003, "model service error"),

    /**
     * 模型服务响应非法。
     */
    MODEL_RESPONSE_INVALID(500004, "model response invalid"),

    /**
     * 模型返回 embedding 为空。
     */
    MODEL_EMBEDDING_EMPTY(500005, "model embedding is empty"),

    /**
     * 模型返回向量维度非法。
     */
    MODEL_DIM_MISMATCH(500006, "model vector dimension mismatch"),

    /**
     * 图片向量化失败。
     */
    IMAGE_VECTORIZATION_FAILED(500007, "image vectorization failed"),

    /**
     * 文本向量化失败。
     */
    TEXT_VECTORIZATION_FAILED(500008, "text vectorization failed"),

    /**
     * 图片输入类型非法。
     */
    IMAGE_INPUT_TYPE_INVALID(500009, "image input type invalid");

    private final Integer code;

    private final String message;
}
