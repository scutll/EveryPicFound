package com.everypicfound.imageasset.domain.service;

import com.everypicfound.imageasset.domain.enums.FailReason;

public interface ImageAssetStatusService {

    // 图片可用。
    void markNormal(Long imageId);

    // 图片逻辑删除。
    void markDeleted(Long imageId);

    // 图片文件缺失或不可读。
    void markInvalid(Long imageId, FailReason failReason);

    // 初始化或重试待处理。
    void markVectorPending(Long imageId);

    // 向量化开始。
    void markVectorProcessing(Long imageId);

    // 向量化成功。
    void markVectorReady(Long imageId);

    // 向量化失败。
    void markVectorFailed(Long imageId, FailReason failReason);

    // 更新重试次数。
    void increaseRetryCount(Long imageId);

    // 处理卡死任务。
    void resetProcessingToPending(Long imageId);
}
