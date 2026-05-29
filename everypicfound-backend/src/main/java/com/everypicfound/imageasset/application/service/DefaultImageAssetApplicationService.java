package com.everypicfound.imageasset.application.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.common.response.PageResult;
import com.everypicfound.imageasset.application.command.ImageAssetQueryCriteria;
import com.everypicfound.imageasset.application.command.ImageAssetSaveCommand;
import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageUploadResult;
import com.everypicfound.imageasset.domain.duplicate.ImageDuplicateChecker;
import com.everypicfound.imageasset.domain.enums.CleanStatus;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.extractor.ImageMetadata;
import com.everypicfound.imageasset.domain.extractor.ImageMetadataExtractor;
import com.everypicfound.imageasset.domain.generator.FileHashCalculator;
import com.everypicfound.imageasset.domain.generator.ImageFileNameGenerator;
import com.everypicfound.imageasset.domain.generator.ImageIdGenerator;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.domain.service.OrphanFileLogService;
import com.everypicfound.imageasset.domain.service.OrphanFileRecord;
import com.everypicfound.imageasset.domain.validator.ImageUploadValidator;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.api.StorageSaveRequest;
import com.everypicfound.storage.api.StoredFile;
import com.everypicfound.storage.error.StorageErrorCode;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;

import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DefaultImageAssetApplicationService implements ImageAssetApplicationService {

    private static final String MODULE = "image-asset";

    private static final String OPERATION_UPLOAD = "IMAGE_UPLOAD";

    private static final String VECTORIZATION_SOURCE = "IMAGE_UPLOAD";

    private final ImageUploadValidator imageUploadValidator;

    private final ImageMetadataExtractor imageMetadataExtractor;

    private final FileHashCalculator fileHashCalculator;

    private final ImageFileNameGenerator imageFileNameGenerator;

    private final ImageIdGenerator imageIdGenerator;

    private final ImageDuplicateChecker imageDuplicateChecker;

    private final ImageAssetRepository imageAssetRepository;

    private final FileStorageService fileStorageService;

    private final VectorizationTaskPublisher vectorizationTaskPublisher;

    private final OrphanFileLogService orphanFileLogService;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    @Override
    public ImageAssetDTO getDetail(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void delete(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    /*
     * upload
     */

    @Override
    public ImageUploadResult upload(ImageUploadCommand command) {
        long startTime = System.currentTimeMillis();

        recordUploadStart(command);

        /*
         * 验证图片文件和生成id、文件名
         */

        //inputStream只能读取一次
        //先把inputStream的内容存入字节数组，后续需要的时候转成放入字节流
        byte[] fileBytes = readFileBytes(command);

        ImageUploadCommand normalizedCommand = rebuildCommand(command, fileBytes);

        imageUploadValidator.validate(normalizedCommand);

        ImageMetadata metadata = imageMetadataExtractor.extract(normalizedCommand);

        String fileHash = fileHashCalculator.calculateHash(new ByteArrayInputStream(fileBytes));

        imageDuplicateChecker.checkDuplicate(fileHash);

        Long imageId = imageIdGenerator.nextId();

        String filename = imageFileNameGenerator.generateFileName(imageId, metadata.getFileExt());

        /*
         * 保存到Storage和入库
         */
        StoredFile storedFile = saveFile(command, metadata, imageId, fileBytes);

        saveImageAsset(command, metadata, fileHash, imageId, filename, storedFile);

        publishVectorizationTask(imageId);

        ImageUploadResult result = buildUploadResult(command, imageId, storedFile);

        recordUploadSuccess(imageId, startTime);

        return result;
    }

    private byte[] readFileBytes(ImageUploadCommand command) {
        if (command == null || command.getInputStream() == null) {
            throw new BizException(ImageAssetErrorCode.IMAGE_EMPTY);
        }

        try {
            return command.getInputStream().readAllBytes();
        } catch (IOException exception) {
            throw new SystemException(CommonErrorCode.SYSTEM_ERROR);
        }
    }

    private ImageUploadCommand rebuildCommand(ImageUploadCommand source, byte[] fileBytes) {
        return ImageUploadCommand.builder()
                .inputStream(new ByteArrayInputStream(fileBytes))
                .originalFileName(source.getOriginalFileName())
                .fileSize(source.getFileSize())
                .mimeType(source.getMimeType())
                .fileHash(source.getFileHash())
                .fileExt(source.getFileExt())
                .width(source.getWidth())
                .height(source.getHeight())
                .imageId(source.getImageId())
                .build();
    }

    private StoredFile saveFile(
            ImageUploadCommand command,
            ImageMetadata metadata,
            Long imageId,
            byte[] fileBytes) {
        StorageSaveRequest request = StorageSaveRequest.builder()
                .inputStream(new ByteArrayInputStream(fileBytes))
                .originalFileName(command.getOriginalFileName())
                .fileExt(metadata.getFileExt())
                .fileSize(metadata.getFileSize())
                .imageId(imageId)
                .mimeType(metadata.getMimeType())
                .build();
        StoredFile storedFile = fileStorageService.save(request);

        if (storedFile == null || storedFile.getStoragePath() == null || storedFile.getStoragePath().isBlank()) {
            throw new SystemException(StorageErrorCode.FILE_SAVE_FAILED);
        }

        return storedFile;
    }

    private void saveImageAsset(
            ImageUploadCommand command,
            ImageMetadata metadata,
            String fileHash,
            Long imageId,
            String fileName,
            StoredFile storedFile) {
        ImageAssetSaveCommand saveCommand = ImageAssetSaveCommand.builder()
                .id(imageId)
                .fileName(resolveFileName(fileName, storedFile))
                .originalFileName(command.getOriginalFileName())
                .fileHash(fileHash)
                .fileSize(metadata.getFileSize())
                .mimeType(metadata.getMimeType())
                .fileExt(metadata.getFileExt())
                .width(metadata.getWidth())
                .height(metadata.getHeight())
                .storagePath(storedFile.getStoragePath())
                .thumbnailPath(null)
                .imageStatus(ImageStatus.NORMAL)
                .vectorStatus(VectorStatus.PENDING)
                .build();

        try {
            boolean saved = imageAssetRepository.save(saveCommand);
            if (!saved) {
                handleMetadataSaveFailed(storedFile, fileHash);
            }
        } catch (DuplicateKeyException exception) {
            compensateSavedFile(storedFile, fileHash);
            throw new BizException(ImageAssetErrorCode.DUPLICATE_IMAGE);
        } catch (RuntimeException exception) {
            compensateSavedFile(storedFile, fileHash);
            throw new SystemException(ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED, exception);
        }

    }

    private String resolveFileName(String generatedFileName, StoredFile storedFile) {
        if (storedFile.getFileName() != null && !storedFile.getFileName().isBlank()) {
            return storedFile.getFileName();
        }

        return generatedFileName;
    }

    private void handleMetadataSaveFailed(StoredFile storedFile, String fileHash) {
        compensateSavedFile(storedFile, fileHash);
        throw new SystemException(ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED);
    }

    private void compensateSavedFile(StoredFile storedFile, String fileHash) {
        boolean deleted = false;

        try {
            deleted = fileStorageService.delete(storedFile.getStoragePath());
        } catch (RuntimeException exception) {
            deleted = false;
        }

        if (!deleted) {
            orphanFileLogService.recordOrphanFile(
                    OrphanFileRecord.builder()
                            .storagePath(storedFile.getStoragePath())
                            .fileHash(fileHash)
                            .failReason(ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED.getMessage())
                            .retryCount(0)
                            .CleanStatus(CleanStatus.WAITING)
                            .build());
        }
    }

    private void publishVectorizationTask(Long imageId) {
        RequestContext context = RequestContextHolder.get();

        ImageVectorizationTaskCommand taskCommand = ImageVectorizationTaskCommand.builder()
                .imageId(imageId)
                .traceId(context == null ? null : context.getTraceId())
                .requestId(context == null ? null : context.getRequestId())
                .source(VECTORIZATION_SOURCE)
                .build();

        VectorizationPublishResult publishResult = vectorizationTaskPublisher.publish(taskCommand);

        if (publishResult == null || !Boolean.TRUE.equals(publishResult.getSuccess())) {
            logService.recordErrorLog(
                    buildLogContext(
                            imageId,
                            "VECTORIZE_TASK_PUBLISH_FAILED",
                            publishResult == null ? "vectorization task publish result is null"
                                    : publishResult.getMessage()));
        }

    }

    private ImageUploadResult buildUploadResult(ImageUploadCommand command, Long imageId, StoredFile storedFile) {
        return ImageUploadResult.builder()
                .imageId(imageId)
                .originalFileName(command.getOriginalFileName())
                .imageUrl(storedFile.getAccessUrl())
                .imageStatus(ImageStatus.NORMAL)
                .vectorStatus(VectorStatus.PENDING)
                .build();
    }

    private void recordUploadStart(ImageUploadCommand command) {
        logService.recordBizLog(
                buildLogContext(null, OPERATION_UPLOAD, command == null ? "image upload start"
                        : "image upload start, fileName:" + command.getOriginalFileName()));

        metricRecorder.increment(
                MetricName.UPLOAD_COST_MS,
                MetricTags.builder()
                        .tags(Map.of("status", "START"))
                        .build());
    }

    private void recordUploadSuccess(Long imageId, long startTime) {
        long costMs = System.currentTimeMillis() - startTime;

        logService.recordSuccessLog(
                buildLogContext(
                        imageId,
                        OPERATION_UPLOAD,
                        "image upload success, costMs: " + costMs));

        metricRecorder.recordTimer(
                MetricName.UPLOAD_COST_MS,
                costMs,
                MetricTags.builder()
                        .tags(Map.of("status", "SUCCESS"))
                        .build());

    }

    private LogContext buildLogContext(Long imageId, String operation, String message) {
        RequestContext context = RequestContextHolder.get();

        return LogContext.builder()
                .requestId(context == null ? null : context.getRequestId())
                .traceId(context == null ? null : context.getTraceId())
                .bizId(imageId == null ? null : String.valueOf(imageId))
                .module(MODULE)
                .operation(operation)
                .message(message)
                .build();
    }

}
