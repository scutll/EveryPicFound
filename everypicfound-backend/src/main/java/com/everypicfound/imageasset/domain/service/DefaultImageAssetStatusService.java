package com.everypicfound.imageasset.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultImageAssetStatusService implements ImageAssetStatusService {

    private final ImageAssetRepository imageAssetRepository;

    // 标记图片正常。
    @Override
    public void markNormal(Long imageId) {
        updateImageStatus(imageId, ImageStatus.NORMAL, null);
    }

    // 逻辑删除图片。
    @Override
    public void markDeleted(Long imageId) {
        updateImageStatus(imageId, ImageStatus.DELETED, null);
    }

    // 标记图片异常。
    @Override
    public void markInvalid(Long imageId, FailReason failReason) {
        updateImageStatus(imageId, ImageStatus.INVALID, failReason);
    }

    // 标记待向量化。
    @Override
    public void markVectorPending(Long imageId) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(VectorStatus.PENDING)
                .build();

        requireUpdated(imageAssetRepository.updateVectorStatus(command),
                VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
    }

    // 标记向量化处理中，并写入 processing_started_time。
    @Override
    public void markVectorProcessing(Long imageId) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(VectorStatus.PROCESSING)
                .processingStartedTime(LocalDateTime.now())
                .build();
        requireUpdated(imageAssetRepository.updateVectorStatus(command),
                VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
    }

    // 标记向量 READY，并写入 vector_updated_time。
    @Override
    public void markVectorReady(Long imageId) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .build();

        boolean updated = imageAssetRepository.updateVectorReady(command);
        requireUpdated(updated, VectorizationErrorCode.VECTOR_READY_UPDATE_FAILED);

    }

    // 标记向量失败，并写入 fail_reason。
    @Override
    public void markVectorFailed(Long imageId, FailReason failReason) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .failReason(failReason)
                .build();

        requireUpdated(imageAssetRepository.updateVectorFailed(command),
                VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
    }

    // 增加重试次数。
    @Override
    public void increaseRetryCount(Long imageId) {
        requireUpdated(
                imageAssetRepository.increaseRetryCount(imageId),
                VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
    }

    // 将超时 PROCESSING 回退为 PENDING。
    @Override
    public void resetProcessingTimeoutToPending(Long imageId) {
        imageAssetRepository.increaseRetryCount(imageId);

        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(VectorStatus.PENDING)
                .failReason(FailReason.PROCESSING_TIMEOUT)
                .build();

        boolean updated = imageAssetRepository.updateVectorStatus(command);
        requireUpdated(updated, VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
    }

    private void updateImageStatus(Long imageId, ImageStatus targetStatus, FailReason failReason) {
        ImageStatusUpdateCommand command = ImageStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(targetStatus)
                .failReason(failReason)
                .build();

        boolean updated = imageAssetRepository.updateImageStatus(command);

        requireUpdated(updated, ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED);
    }

    private void requireUpdated(boolean updated, ErrorCode errorCode) {
        if (!updated) {
            throw new SystemException(errorCode);
        }
    }
}
