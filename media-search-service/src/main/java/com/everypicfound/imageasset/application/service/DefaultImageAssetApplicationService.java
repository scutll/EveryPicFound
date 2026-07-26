package com.everypicfound.imageasset.application.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;
import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogContext;
import com.everypicfound.common.log.LogEventName;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.log.LogStatus;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
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

@Service
@RequiredArgsConstructor
public class DefaultImageAssetApplicationService implements ImageAssetApplicationService {

    /**
     * 仅当统一日志系统自身发生异常时使用。
     * 正常业务日志必须通过 LogService 输出。
     */
    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(
            DefaultImageAssetApplicationService.class);

    private static final String MODULE = "image-asset";

    private static final String BIZ_TYPE_IMAGE_UPLOAD = "IMAGE_UPLOAD";

    private static final String OPERATION_COMPENSATE_DELETE = "compensate-file-delete";

    /**
     * 异步任务来源。
     *
     * 指标标签使用有限枚举值，避免出现任意字符串。
     */
    private static final String VECTORIZATION_SOURCE = "image_upload";

    /*
     * ============================================================
     * 通用指标结果
     * ============================================================
     */

    private static final String RESULT_SUCCESS = "success";

    private static final String RESULT_FAILED = "failed";

    private static final String RESULT_REJECTED = "rejected";

    /*
     * ============================================================
     * 上传阶段
     * ============================================================
     */

    private static final String STAGE_VALIDATE = "validate";

    private static final String STAGE_METADATA_EXTRACT = "metadata_extract";

    private static final String STAGE_HASH = "hash";

    private static final String STAGE_DUPLICATE_CHECK = "duplicate_check";

    private static final String STAGE_STORAGE_SAVE = "storage_save";

    private static final String STAGE_METADATA_SAVE = "metadata_save";

    private static final String STAGE_PUBLISH = "publish";

    /*
     * ============================================================
     * 补偿原因
     * ============================================================
     */

    private static final String COMPENSATION_REASON_DUPLICATE = "duplicate";

    private static final String COMPENSATION_REASON_METADATA_SAVE = "metadata_save_failed";

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
    public boolean exists(Long imageId) {
        return imageId != null
                && imageId > 0
                && imageAssetRepository.existsById(imageId);
    }

    @Override
    public PageResult<ImageAssetDTO> pageQuery(ImageAssetQueryCriteria criteria) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void delete(Long imageId) {
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * 图片上传完整用例。
     *
     * <p>主要步骤：</p>
     *
     * <ol>
     *     <li>读取并校验上传内容</li>
     *     <li>提取图片元数据</li>
     *     <li>计算文件 Hash 并执行重复检查</li>
     *     <li>保存文件</li>
     *     <li>保存图片资产元数据</li>
     *     <li>发布异步向量化任务</li>
     * </ol>
     *
     * <p>
     * 异步任务发布失败不回滚已经完成的上传。
     * 此时图片保持 PENDING 状态，可由后续扫描或补偿机制重新发布。
     * </p>
     */
    @Override
    public ImageUploadResult upload(ImageUploadCommand command) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try{
            /*
             * InputStream 只能可靠读取一次。
             * 因此先转换成 byte[]，后续每个阶段重新构造输入流。
             */

            PreparedUpload preparedUpload = observeUploadStage(STAGE_VALIDATE, ()->prepareUpload(command));

            recordUploadFileSize(preparedUpload.fileBytes.length);


            ImageMetadata metadata = observeUploadStage(STAGE_METADATA_EXTRACT, ()->imageMetadataExtractor.extract(preparedUpload.command));

            String fileHash = observeUploadStage(STAGE_HASH, ()->fileHashCalculator.calculateHash(new ByteArrayInputStream(preparedUpload.fileBytes))); 

            observeUploadStage(STAGE_DUPLICATE_CHECK, ()->{
                imageDuplicateChecker.checkDuplicate(fileHash);
                 return null;
            });

            Long imageId = imageIdGenerator.nextId();

            String filename = imageFileNameGenerator.generateFileName(imageId, metadata.getFileExt());
            
            /*
             * 保存到Storage和入库
             */
            StoredFile sotrreFile = observeUploadStage(STAGE_STORAGE_SAVE, ()->saveFile(command, metadata, imageId, preparedUpload.fileBytes));

            observeUploadStage(STAGE_METADATA_SAVE, ()->{
                saveImageAsset(command, metadata, fileHash, imageId, filename, sotrreFile);
                return null;
            });

            /*
             * 发布结果会记录到 publish 阶段指标。
             *
             * 即使发布失败，Publisher 已经负责记录错误，
             * 当前上传用例仍然返回成功。
             */

            observePublishStage(imageId);

            ImageUploadResult uploadResult = buildUploadResult(preparedUpload.command, imageId, sotrreFile);

            result = RESULT_SUCCESS;

            return uploadResult;
        } catch(BizException exception){
            /*
             * 参数非法、重复图片等属于业务拒绝。
             *
             * 这里只记录指标，具体业务拒绝事件由
             * GlobalExceptionHandler 统一记录。
             */
            result = RESULT_REJECTED;

            recordUploadRejection(exception);

            throw exception;

        } finally{
            /*
             * SystemException 或未知 RuntimeException 未被捕获时，
             * result 会保持 failed，并继续向上抛出。
             */
            recordUploadMetrics(
                    result,
                    startNanos);
        }

    }

    /**
     * 完成上传输入准备。
     *
     * validate 阶段包含：
     * 1. 读取上传流；
     * 2. 重建可重复读取的命令；
     * 3. 执行上传参数校验。
     */
    private PreparedUpload prepareUpload(
            ImageUploadCommand command) {

        byte[] fileBytes = readFileBytes(command);

        ImageUploadCommand normalizedCommand = rebuildCommand(
                command,
                fileBytes);

        imageUploadValidator.validate(normalizedCommand);

        return new PreparedUpload(
                fileBytes,
                normalizedCommand);
    }

    /**
     * 将上传输入流读取到内存。
     *
     * IOException 属于系统异常，转换时必须保留 cause。
     */
    private byte[] readFileBytes(ImageUploadCommand command) {
        if (command == null || command.getInputStream() == null) {
            throw new BizException(ImageAssetErrorCode.IMAGE_EMPTY);
        }

        try {
            return command.getInputStream().readAllBytes();
        } catch (IOException exception) {
            throw new SystemException(CommonErrorCode.SYSTEM_ERROR, exception);
        }
    }

    /**
     * 使用内存字节重新构造上传命令。
     *
     * 后续元数据提取、Hash 计算和文件保存均使用新的输入流，
     * 避免重复读取原始 InputStream。
     */
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

    /**
     * 保存图片文件。
     *
     * Storage 内部发生 IOException 时，应由 Storage Adapter
     * 转换为带 cause 的 SystemException 并继续向上传播。
     */
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

            /*
             * 没有底层 Throwable，只存在明确的失败结果。
             */
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

        boolean saved;

        try {
            saved = imageAssetRepository.save(saveCommand);

        } catch (DuplicateKeyException exception) {
            /*
             * 数据库唯一键冲突属于预期业务拒绝。
             * 文件已经保存，需要先执行删除补偿。
             */
            compensateSavedFile(storedFile, fileHash, imageId, command, COMPENSATION_REASON_DUPLICATE);

            throw new BizException(ImageAssetErrorCode.DUPLICATE_IMAGE, exception);
        } catch (RuntimeException exception) {
            /*
             * 其他数据库异常属于系统失败。
             * 补偿后继续向上传递，并保留原始 cause。
             */
            compensateSavedFile(storedFile, fileHash, imageId, command, COMPENSATION_REASON_METADATA_SAVE);

            throw new SystemException(ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED, exception);
        }

        if (!saved) {
            compensateSavedFile(storedFile, fileHash, imageId, command, COMPENSATION_REASON_METADATA_SAVE);

            throw new SystemException(ImageAssetErrorCode.IMAGE_METADATA_SAVE_FAILED);
        }
    }

    /**
     * 元数据保存失败后的文件删除补偿。
     *
     * <p>
     * 删除异常在这里被转换为补偿失败结果，不再继续向上传播，
     * 因此当前方法是该删除异常的最终日志责任边界。
     * </p>
     */
    private void compensateSavedFile(
            StoredFile storedFile,
            String fileHash,
            Long imageId,
            ImageUploadCommand command,
            String reason) {
        boolean deleted = false;
        RuntimeException deleteException = null;

        try {
            deleted = fileStorageService.delete(storedFile.getStoragePath());
        } catch (RuntimeException exception) {
            /*
             * 不能让补偿删除异常覆盖原始的元数据保存异常。
             *
             * 因此先保存异常，随后记录错误和孤儿文件事件，
             * 但不再向外抛出这个删除异常。
             */
            deleteException = exception;
        }

        recordCompensationMetric(
                deleted,
                reason);

        if (deleted) {
            return;
        }

        /*
         * 删除异常或明确的删除失败结果在这里终止传播，
         * 所以需要由当前边界记录一次错误。
         */
        recordCompensationFailureSafely(
                imageId,
                deleteException);

        /*
         * 孤儿文件属于需要后续处理的状态事件。
         * OrphanFileLogService 负责记录 ORPHAN_FILE_DETECTED。
         */
        recordOrphanFileSafely(
                OrphanFileRecord.builder()
                        .imageId(imageId)
                        .storagePath(
                                storedFile.getStoragePath())
                        .accessUrl(
                                storedFile.getAccessUrl())
                        .fileName(
                                storedFile.getFileName())
                        .originalFileName(
                                command == null
                                        ? null
                                        : command
                                                .getOriginalFileName())
                        .fileHash(fileHash)
                        .failReason(reason)
                        .retryCount(0)
                        .cleanStatus(CleanStatus.WAITING)
                        .build());
    }

    /**
     * 发布异步向量化任务并记录上传内部 publish 阶段耗时。
     *
     * Publisher 自己负责：
     * 1. 发布次数和发布耗时；
     * 2. 线程池拒绝指标；
     * 3. 发布失败错误日志。
     *
     * 当前上传层只记录阶段结果，不重复记录错误。
     */
    private void observePublishStage(Long imageId) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            VectorizationPublishResult publishResult = publishVectorizationTask(imageId);

            if (publishResult != null
                    && Boolean.TRUE.equals(
                            publishResult.getSuccess())) {

                result = RESULT_SUCCESS;
            }
        } finally {
            recordUploadStageMetric(
                    STAGE_PUBLISH,
                    result,
                    startNanos);
        }
    }

    /**
     * 构造异步任务命令并调用任务发布器。
     */
    private VectorizationPublishResult publishVectorizationTask(Long imageId) {
        RequestContext context = RequestContextHolder.get();

        ImageVectorizationTaskCommand taskCommand = ImageVectorizationTaskCommand.builder()
                .imageId(imageId)
                .traceId(context == null ? null : context.getTraceId())
                .requestId(context == null ? null : context.getRequestId())
                .source(VECTORIZATION_SOURCE)
                .build();

        return vectorizationTaskPublisher.publish(taskCommand);

    }

    /**
     * 构建上传返回结果。
     */
    private ImageUploadResult buildUploadResult(ImageUploadCommand command, Long imageId, StoredFile storedFile) {
        return ImageUploadResult.builder()
                .imageId(imageId)
                .originalFileName(command.getOriginalFileName())
                .imageUrl(storedFile.getAccessUrl())
                .imageStatus(ImageStatus.NORMAL)
                .vectorStatus(VectorStatus.PENDING)
                .build();
    }


    /**
     * Storage 返回文件名时优先使用 Storage 结果；
     * 否则使用业务层生成的文件名。
     */
    private String resolveFileName(
            String generatedFileName,
            StoredFile storedFile) {

        if (storedFile.getFileName() != null
                && !storedFile.getFileName().isBlank()) {

            return storedFile.getFileName();
        }

        return generatedFileName;
    }

    /*
     * ============================================================
     * 上传指标
     * ============================================================
     */

    /**
     * 记录一个上传内部阶段的耗时。
     *
     * BizException 表示业务拒绝；
     * SystemException 或其他异常保持 failed 并继续向上传播。
     */
    private <T> T observeUploadStage(
            String stage,
            Supplier<T> action) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            T value = action.get();
            result = RESULT_SUCCESS;

            return value;
        } catch (BizException exception) {
            result = RESULT_REJECTED;
            throw exception;
        } finally {
            recordUploadStageMetric(
                    stage,
                    result,
                    startNanos);
        }
    }

    /**
     * 上传用例总次数和总耗时。
     */
    private void recordUploadMetrics(
            String result,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.IMAGE_UPLOAD_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.IMAGE_UPLOAD_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    /**
     * 上传内部阶段耗时。
     */
    private void recordUploadStageMetric(
            String stage,
            String result,
            long startNanos) {

        metricRecorder.recordTimer(
                MetricName.IMAGE_UPLOAD_STAGE_DURATION,
                elapsedMillis(startNanos),
                MetricTags.builder()
                        .tag(MetricTag.STAGE, stage)
                        .tag(MetricTag.RESULT, result)
                        .build());
    }

    /**
     * 上传文件大小分布。
     */
    private void recordUploadFileSize(long fileSize) {
        if (fileSize < 0L) {
            return;
        }

        metricRecorder.recordValue(
                MetricName.IMAGE_UPLOAD_FILE_SIZE,
                fileSize,
                MetricTags.empty());
    }

    /**
     * 业务拒绝次数。
     *
     * 重复图片除了进入 rejection 指标，
     * 还额外进入 duplicate 专用指标。
     */
    private void recordUploadRejection(
            BizException exception) {

        ErrorCode errorCode = exception == null
                ? null
                : exception.getErrorCode();

        metricRecorder.increment(
                MetricName.IMAGE_UPLOAD_REJECTIONS,
                MetricTags.builder()
                        .tag(
                                MetricTag.REASON,
                                resolveReason(errorCode))
                        .build());

        if (ImageAssetErrorCode.DUPLICATE_IMAGE
                .equals(errorCode)) {

            metricRecorder.increment(
                    MetricName.IMAGE_UPLOAD_DUPLICATES,
                    MetricTags.empty());
        }
    }

    /**
     * 文件删除补偿次数。
     */
    private void recordCompensationMetric(
            boolean deleted,
            String reason) {

        metricRecorder.increment(
                MetricName.IMAGE_UPLOAD_COMPENSATIONS,
                MetricTags.builder()
                        .tag(
                                MetricTag.RESULT,
                                deleted
                                        ? RESULT_SUCCESS
                                        : RESULT_FAILED)
                        .tag(
                                MetricTag.REASON,
                                reason)
                        .build());
    }

    /*
     * ============================================================
     * 补偿错误与孤儿文件
     * ============================================================
     */

    /**
     * 文件补偿删除失败的错误日志。
     *
     * Throwable 存在时记录完整堆栈；
     * delete() 只返回 false 时记录明确失败结果。
     */
    private void recordCompensationFailureSafely(
            Long imageId,
            Throwable throwable) {

        RequestContext requestContext = RequestContextHolder.get();

        LogContext logContext = LogContext.builder()
                .requestId(
                        requestContext == null
                                ? null
                                : requestContext.getRequestId())
                .traceId(
                        requestContext == null
                                ? null
                                : requestContext.getTraceId())
                .bizId(
                        imageId == null
                                ? null
                                : String.valueOf(imageId))
                .bizType(BIZ_TYPE_IMAGE_UPLOAD)
                .module(MODULE)
                .operation(OPERATION_COMPENSATE_DELETE)
                .eventName(
                        LogEventName.SYSTEM_EXCEPTION_OCCURRED)
                .status(LogStatus.FAILED)
                .errorCode(String.valueOf(
                        ImageAssetErrorCode.ORPHAN_FILE_DELETE_FAILED
                                .getCode()))
                .message(
                        ImageAssetErrorCode.ORPHAN_FILE_DELETE_FAILED
                                .getMessage())
                .build();

        try {
            if (throwable == null) {
                logService.recordError(logContext);
            } else {
                logService.recordError(
                        logContext,
                        throwable);
            }
        } catch (RuntimeException loggingException) {
            /*
             * 日志系统故障不能覆盖上传链路原本的异常。
             * 只有此时才允许直接使用底层 Logger。
             */
            if (throwable != null) {
                FALLBACK_LOGGER.error(
                        "Upload compensation delete failed, imageId={}",
                        imageId,
                        throwable);
            }

            FALLBACK_LOGGER.error(
                    "Failed to record upload compensation error, imageId={}",
                    imageId,
                    loggingException);
        }
    }

    /**
     * 孤儿文件记录本身也属于观测逻辑，
     * 不能因为日志模块异常而覆盖原始数据库异常。
     */
    private void recordOrphanFileSafely(
            OrphanFileRecord record) {

        try {
            orphanFileLogService.recordOrphanFile(
                    record);
        } catch (RuntimeException loggingException) {
            FALLBACK_LOGGER.error(
                    "Failed to record orphan file event, imageId={}",
                    record == null
                            ? null
                            : record.getImageId(),
                    loggingException);
        }
    }

    /**
     * 将错误码转换成有限、稳定的指标标签。
     *
     * 不使用 exception.getMessage()，避免高基数标签。
     */
    private String resolveReason(
            ErrorCode errorCode) {

        if (errorCode == null) {
            return "unknown";
        }

        if (errorCode instanceof Enum<?> enumErrorCode) {
            return enumErrorCode.name()
                    .toLowerCase(Locale.ROOT);
        }

        return String.valueOf(
                errorCode.getCode());
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

    /**
     * 上传准备阶段的内部数据。
     *
     * 不作为对外 DTO，仅用于当前 Application Service 内部编排。
     */
    private static final class PreparedUpload {

        private final byte[] fileBytes;

        private final ImageUploadCommand command;

        private PreparedUpload(
                byte[] fileBytes,
                ImageUploadCommand command) {

            this.fileBytes = fileBytes;
            this.command = command;
        }
    }

}
