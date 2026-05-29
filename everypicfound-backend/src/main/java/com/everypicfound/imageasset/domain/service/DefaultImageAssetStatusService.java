package com.everypicfound.imageasset.domain.service;

import org.springframework.stereotype.Service;

import com.everypicfound.imageasset.domain.enums.FailReason;



@Service
public class DefaultImageAssetStatusService implements ImageAssetStatusService {


    // 标记图片正常。
    @Override
    public void markNormal(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 逻辑删除图片。
    @Override
    public void markDeleted(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记图片异常。
    @Override
    public void markInvalid(Long imageId, FailReason failReason) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记待向量化。
    @Override
    public void markVectorPending(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记向量化处理中，并写入 processing_started_time。
    @Override
    public void markVectorProcessing(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记向量 READY，并写入 vector_updated_time。
    @Override
    public void markVectorReady(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记向量失败，并写入 fail_reason。
    @Override
    public void markVectorFailed(Long imageId, FailReason failReason) {
        throw new UnsupportedOperationException("TODO");
    }

    // 增加重试次数。
    @Override
    public void increaseRetryCount(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 将超时 PROCESSING 回退为 PENDING。
    @Override
    public void resetProcessingToPending(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }
}
