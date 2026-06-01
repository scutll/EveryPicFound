package com.everypicfound.imageasset.domain.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.everypicfound.imageasset.application.command.ImageStatusUpdateCommand;
import com.everypicfound.imageasset.application.command.VectorStatusUpdateCommand;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;

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
        imageAssetRepository.updateVectorStatus(command);
    }

    // 标记向量化处理中，并写入 processing_started_time。
    @Override
    public void markVectorProcessing(Long imageId) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(VectorStatus.PROCESSING)
                .processingStartedTime(LocalDateTime.now())
                .build();
        imageAssetRepository.updateVectorStatus(command);
    }

    // 标记向量 READY，并写入 vector_updated_time。
    @Override
    public void markVectorReady(Long imageId) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .build();

        imageAssetRepository.updateVectorReady(command);
    }

    // 标记向量失败，并写入 fail_reason。
    @Override
    public void markVectorFailed(Long imageId, FailReason failReason) {
        VectorStatusUpdateCommand command = VectorStatusUpdateCommand.builder()
                .imageId(imageId)
                .failReason(failReason)
                .build();

        imageAssetRepository.updateVectorFailed(command);
    }

    // 增加重试次数。
    @Override
    public void increaseRetryCount(Long imageId) {
        imageAssetRepository.increaseRetryCount(imageId);
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

        imageAssetRepository.updateVectorStatus(command);
    }

    private void updateImageStatus(Long imageId, ImageStatus targetStatus, FailReason failReason) {
        ImageStatusUpdateCommand command = ImageStatusUpdateCommand.builder()
                .imageId(imageId)
                .targetStatus(targetStatus)
                .failReason(failReason)
                .build();

        imageAssetRepository.updateImageStatus(command);
    }
}
