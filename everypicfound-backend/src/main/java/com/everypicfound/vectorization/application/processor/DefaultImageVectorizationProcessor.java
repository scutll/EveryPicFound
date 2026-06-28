package com.everypicfound.vectorization.application.processor;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.BizException;
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
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.service.ImageAssetQueryService;
import com.everypicfound.imageasset.domain.service.ImageAssetStatusService;
import com.everypicfound.modelclient.api.ModelVectorizationClient;
import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;
import com.everypicfound.modelclient.domain.enums.ImageInputType;
import com.everypicfound.modelclient.error.ModelClientErrorCode;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.error.StorageErrorCode;
import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.collection.ActiveCollectionResolver;
import com.everypicfound.vectorindex.collection.VectorCollectionConfig;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorindex.domain.VectorPayload;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.config.VectorizationProperties;
import com.everypicfound.vectorization.domain.failure.VectorizationFailureHandler;
import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;
import com.everypicfound.vectorization.domain.retry.VectorizationRetryPolicy;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultImageVectorizationProcessor
        implements ImageVectorizationProcessor {

    private static final String MODULE = "vectorization";

    private static final String BIZ_TYPE = "IMAGE_VECTORIZATION";

    private static final String OPERATION_PROCESS = "process";

    private static final String OPERATION_MARK_READY = "mark-ready";

    private static final String RESULT_SUCCESS = "success";

    private static final String RESULT_FAILED = "failed";

    private static final String RESULT_SKIPPED = "skipped";

    private static final String RESULT_RETRYING = "retrying";

    private static final String RESULT_NOT_FOUND = "not_found";

    private static final String RESULT_MISSING = "missing";

    private static final String RESULT_TIMEOUT = "timeout";

    private static final String RESULT_UNAVAILABLE = "unavailable";

    private static final String RESULT_INVALID_RESPONSE = "invalid_response";

    private static final String REASON_NONE = "none";

    private static final String REASON_UNKNOWN = "unknown";

    private static final String REASON_TASK_INVALID = "task_invalid";

    private static final String REASON_ASSET_NOT_FOUND = "asset_not_found";

    private static final String REASON_STATUS_NOT_ALLOWED = "status_not_allowed";

    private static final String REASON_CONCURRENT_PROCESSING = "concurrent_processing";

    private static final String SOURCE_IMAGE_UPLOAD = "image_upload";

    private static final String SOURCE_SCANNER = "scanner";

    private static final String SOURCE_MANUAL = "manual";

    private static final String SOURCE_UNKNOWN = "unknown";

    private static final String STAGE_ASSET_QUERY = "asset_query";

    private static final String STAGE_MARK_PROCESSING = "mark_processing";

    private static final String STAGE_STORAGE_CHECK = "storage_check";

    private static final String STAGE_STORAGE_READ = "storage_read";

    private static final String STAGE_RESOLVE_COLLECTION = "resolve_collection";

    private static final String STAGE_MODEL_INFERENCE = "model_inference";

    private static final String STAGE_DIMENSION_VALIDATE = "dimension_validate";

    private static final String STAGE_VECTOR_UPSERT = "vector_upsert";

    private static final String STAGE_MARK_READY = "mark_ready";

    private final ImageAssetQueryService imageAssetQueryService;

    private final ImageAssetStatusService imageAssetStatusService;

    private final FileStorageService fileStorageService;

    private final ModelVectorizationClient modelVectorizationClient;

    private final VectorIndexClient vectorIndexClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final VectorizationRetryPolicy retryPolicy;

    private final VectorizationFailureHandler failureHandler;

    private final VectorizationProperties properties;

    private final LogService logService;

    private final MetricRecorder metricRecorder;

    @Override
    public ImageVectorizationResult process(
            ImageVectorizationTaskCommand command) {

        long startNanos = System.nanoTime();

        String source = resolveSource(command);

        TaskOutcome outcome = new TaskOutcome();

        ImageAssetDTO imageAsset = null;

        try {
            if (command == null || command.getImageId() == null) {
                outcome.failed(REASON_TASK_INVALID);

                recordProcessorFailureSafely(
                        command,
                        null,
                        VectorizationErrorCode.VECTORIZATION_TASK_INVALID,
                        null);

                return buildFailedResult(
                        null,
                        null,
                        VectorizationErrorCode.VECTORIZATION_TASK_INVALID
                                .getMessage());
            }

            Long imageId = command.getImageId();

            imageAsset = queryImageAsset(imageId);

            if (imageAsset == null) {
                outcome.skipped(REASON_ASSET_NOT_FOUND);

                recordProcessorFailureSafely(
                        command,
                        imageId,
                        VectorizationErrorCode.IMAGE_ASSET_NOT_FOUND,
                        null);

                return buildAssetNotFoundResult(imageId);
            }

            if (!canStartVectorization(imageAsset)) {
                outcome.skipped(
                        resolveSkippedReason(imageAsset));

                return buildSkippedResult(imageAsset);
            }

            ImageAssetDTO currentImageAsset = imageAsset;

            observeStage(
                    STAGE_MARK_PROCESSING,
                    () -> {
                        imageAssetStatusService
                                .markVectorProcessing(imageId);

                        return null;
                    });

            boolean fileExists = checkStorageExists(
                    currentImageAsset.getStoragePath());

            if (!fileExists) {
                metricRecorder.increment(
                        MetricName.STORAGE_MISSING_FILES,
                        MetricTags.builder()
                                .tag(
                                        MetricTag.SOURCE,
                                        "vectorization")
                                .build());

                VectorizationFailureContext failureContext = buildFailureContext(
                        currentImageAsset,
                        command,
                        FailReason.FILE_NOT_FOUND,
                        VectorizationErrorCode.IMAGE_FILE_NOT_FOUND
                                .getMessage(),
                        null);

                ImageVectorizationResult failureResult = failureHandler.handleFileMissing(
                        failureContext);

                outcome.failed(
                        normalizeReason(
                                FailReason.FILE_NOT_FOUND));

                return failureResult;
            }

            StorageResource storageResource = observeStage(
                    STAGE_STORAGE_READ,
                    () -> requireStorageResource(
                           readStorageResource(currentImageAsset, command)));

            VectorCollectionConfig collectionConfig = observeStage(
                    STAGE_RESOLVE_COLLECTION,
                    () -> requireCollectionConfig(
                            activeCollectionResolver
                                    .resolveActiveCollection()));

            VectorizeResult vectorizeResult = observeStage(
                    STAGE_MODEL_INFERENCE,
                    () -> requireVectorizeSuccess(
                            vectorizeImage(
                                    currentImageAsset,
                                    storageResource,
                                    collectionConfig,
                                    command)));

            observeStage(
                    STAGE_DIMENSION_VALIDATE,
                    () -> {
                        validateVectorDimension(
                                vectorizeResult,
                                collectionConfig);

                        return null;
                    });

            observeStage(
                    STAGE_VECTOR_UPSERT,
                    () -> {
                        VectorOperationResult upsertResult = upsertVector(
                                currentImageAsset,
                                vectorizeResult,
                                collectionConfig);

                        requireUpsertSuccess(upsertResult);

                        return null;
                    });

            markReadyAfterVectorUpsert(
                    imageId,
                    command);

            outcome.success();

            return buildSuccessResult(imageId);
        } catch (RuntimeException exception) {
            Long imageId = command == null
                    ? null
                    : command.getImageId();

            ErrorCode resolvedErrorCode = resolveErrorCode(exception);
            ErrorCode errorCode = resolvedErrorCode == null
                    ? VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED
                    : resolvedErrorCode;

            FailReason failReason = mapFailReason(exception);

            /*
             * Processor 是异步链路最终 Throwable 责任点。
             */
            recordProcessorFailureSafely(
                    command,
                    imageId,
                    errorCode,
                    exception);

            ImageVectorizationResult failureResult = handleFailure(
                    imageAsset,
                    command,
                    failReason,
                    errorCode.getMessage(),
                    exception);
            outcome.fromFailureResult(failureResult, failReason);

            return failureResult;
        } finally {
            recordTaskMetrics(
                    source,
                    outcome,
                    startNanos);
        }
    }

    /**
     * 图片资产查询需要区分正常返回、记录不存在和查询异常。
     */
    private ImageAssetDTO queryImageAsset(Long imageId) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            ImageAssetDTO imageAsset = imageAssetQueryService.getById(imageId);

            result = imageAsset == null
                    ? RESULT_NOT_FOUND
                    : RESULT_SUCCESS;

            return imageAsset;
        } finally {
            recordStageMetrics(
                    STAGE_ASSET_QUERY,
                    result,
                    startNanos);
        }
    }

    private boolean canStartVectorization(
            ImageAssetDTO imageAsset) {

        return ImageStatus.NORMAL.equals(
                imageAsset.getImageStatus())
                && VectorStatus.PENDING.equals(
                        imageAsset.getVectorStatus());
    }

    private String resolveSkippedReason(
            ImageAssetDTO imageAsset) {

        if (imageAsset != null
                && VectorStatus.PROCESSING.equals(
                        imageAsset.getVectorStatus())) {

            return REASON_CONCURRENT_PROCESSING;
        }

        return REASON_STATUS_NOT_ALLOWED;
    }

    private boolean checkStorageExists(
            String storagePath) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            boolean exists = fileStorageService.exists(storagePath);

            result = exists
                    ? RESULT_SUCCESS
                    : RESULT_MISSING;

            return exists;
        } finally {
            recordStageMetrics(
                    STAGE_STORAGE_CHECK,
                    result,
                    startNanos);
        }
    }

    private StorageResource readStorageResource(ImageAssetDTO imageAsset, ImageVectorizationTaskCommand command) {
        try{
            return fileStorageService.read(imageAsset.getStoragePath());
        } catch (SystemException exception) {
            /*
             * Storage 适配层已经提供了明确 ErrorCode，
             * 不要再次包成通用文件读取异常。
             */
            throw exception;
        } catch (RuntimeException exception) {
            throw new SystemException(VectorizationErrorCode.IMAGE_FILE_READ_FAILED, exception);
        }
    }

    private StorageResource requireStorageResource(
            StorageResource storageResource) {

        if (storageResource == null
                || storageResource.getInputStream() == null) {

            throw new SystemException(
                    VectorizationErrorCode.IMAGE_FILE_READ_FAILED);
        }

        return storageResource;
    }

    private VectorCollectionConfig requireCollectionConfig(
            VectorCollectionConfig config) {

        if (config == null
                || config.getCollectionName() == null
                || config.getCollectionName().isBlank()
                || config.getModelName() == null
                || config.getModelName().isBlank()
                || config.getVectorDim() == null
                || config.getVectorDim() <= 0) {

            throw new SystemException(
                    VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID);
        }

        return config;
    }

    private VectorizeResult vectorizeImage(
            ImageAssetDTO imageAsset,
            StorageResource storageResource,
            VectorCollectionConfig collectionConfig,
            ImageVectorizationTaskCommand command) {

        ImageVectorizeRequest request = ImageVectorizeRequest.builder()
                .imageInputType(
                        ImageInputType.MULTIPART)
                .imageId(imageAsset.getId())
                .storagePath(
                        imageAsset.getStoragePath())
                .inputStream(
                        storageResource.getInputStream())
                .originalFileName(
                        imageAsset.getOriginalFileName())
                .fileSize(
                        storageResource.getFileSize())
                .mimeType(
                        storageResource.getMimeType())
                .modelName(
                        collectionConfig.getModelName())
                .traceId(command.getTraceId())
                .requestId(command.getRequestId())
                .build();

        return modelVectorizationClient.vectorizeImage(
                request);
    }

    private VectorizeResult requireVectorizeSuccess(
            VectorizeResult result) {

        if (result == null) {
            throw new SystemException(
                    VectorizationErrorCode.MODEL_VECTORIZATION_FAILED);
        }

        if (!Boolean.TRUE.equals(result.getSuccess())) {
            ErrorCode errorCode = result.getErrorCode() == null
                    ? VectorizationErrorCode.MODEL_VECTORIZATION_FAILED
                    : result.getErrorCode();

            throw new SystemException(errorCode);
        }

        if (result.getEmbedding() == null
                || result.getEmbedding().isEmpty()) {

            throw new SystemException(
                    ModelClientErrorCode.MODEL_EMBEDDING_EMPTY);
        }

        if (result.getDim() == null
                || result.getDim() <= 0) {

            throw new SystemException(
                    ModelClientErrorCode.MODEL_DIM_MISMATCH);
        }

        return result;
    }

    private void validateVectorDimension(
            VectorizeResult vectorizeResult,
            VectorCollectionConfig collectionConfig) {

        if (vectorizeResult == null
                || vectorizeResult.getDim() == null
                || collectionConfig == null
                || collectionConfig.getVectorDim() == null
                || !vectorizeResult.getDim().equals(
                        collectionConfig.getVectorDim())) {

            metricRecorder.increment(
                    MetricName.VECTORIZATION_DIMENSION_MISMATCH,
                    MetricTags.builder()
                            .tag(
                                    MetricTag.SOURCE,
                                    "image_vectorization")
                            .build());

            throw new SystemException(
                    VectorizationErrorCode.VECTOR_DIM_MISMATCH);
        }
    }

    private VectorOperationResult upsertVector(
            ImageAssetDTO imageAsset,
            VectorizeResult vectorizeResult,
            VectorCollectionConfig collectionConfig) {

        VectorUpsertRequest request = VectorUpsertRequest.builder()
                .collectionName(
                        collectionConfig
                                .getCollectionName())
                .vectorId(imageAsset.getId())
                .embedding(
                        vectorizeResult.getEmbedding())
                .payload(
                        VectorPayload.builder()
                                .createdTime(
                                        LocalDateTime.now())
                                .build())
                .build();

        return vectorIndexClient.upsert(request);
    }

    private void requireUpsertSuccess(
            VectorOperationResult result) {

        if (result == null) {
            throw new SystemException(
                    VectorizationErrorCode.VECTOR_UPSERT_FAILED);
        }

        if (!Boolean.TRUE.equals(result.getSuccess())) {
            ErrorCode errorCode = result.getErrorCode() == null
                    ? VectorizationErrorCode.VECTOR_UPSERT_FAILED
                    : result.getErrorCode();

            throw new SystemException(errorCode);
        }
    }

    /**
     * 此方法执行时，Qdrant Upsert 已经成功。
     * SQL READY 更新失败会形成跨存储不一致。
     */
    private void markReadyAfterVectorUpsert(
            Long imageId,
            ImageVectorizationTaskCommand command) {

        try {
            observeStage(
                    STAGE_MARK_READY,
                    () -> {
                        imageAssetStatusService
                                .markVectorReady(imageId);

                        return null;
                    });
        } catch (RuntimeException exception) {
            metricRecorder.increment(
                    MetricName.VECTORIZATION_READY_UPDATE_FAILURES,
                    MetricTags.empty());

            recordReadyCompensationEventSafely(
                    imageId,
                    command);

            /*
             * 保留原始异常，交给 process catch 统一记录错误。
             */
            throw exception;
        }
    }

    private <T> T observeStage(
            String stage,
            Supplier<T> action) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            T value = action.get();
            result = RESULT_SUCCESS;
            return value;
        } catch (RuntimeException exception) {
            result = resolveStageFailureResult(
                    stage,
                    exception);

            throw exception;
        } finally {
            recordStageMetrics(
                    stage,
                    result,
                    startNanos);
        }
    }

    private String resolveStageFailureResult(
            String stage,
            RuntimeException exception) {

        if (!STAGE_MODEL_INFERENCE.equals(stage)) {
            return RESULT_FAILED;
        }

        ErrorCode errorCode = resolveErrorCode(exception);

        if (ModelClientErrorCode.MODEL_SERVICE_TIMEOUT
                .equals(errorCode)) {

            return RESULT_TIMEOUT;
        }

        if (ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE
                .equals(errorCode)) {

            return RESULT_UNAVAILABLE;
        }

        if (ModelClientErrorCode.MODEL_RESPONSE_INVALID
                .equals(errorCode)
                || ModelClientErrorCode.MODEL_EMBEDDING_EMPTY
                        .equals(errorCode)
                || ModelClientErrorCode.MODEL_DIM_MISMATCH
                        .equals(errorCode)) {

            return RESULT_INVALID_RESPONSE;
        }

        return RESULT_FAILED;
    }

    private void recordStageMetrics(
            String stage,
            String result,
            long startNanos) {

        metricRecorder.recordTimer(
                MetricName.VECTORIZATION_STAGE_DURATION,
                elapsedMillis(startNanos),
                MetricTags.builder()
                        .tag(MetricTag.STAGE, stage)
                        .tag(MetricTag.RESULT, result)
                        .build());
    }

    private ImageVectorizationResult handleFailure(
            ImageAssetDTO imageAsset,
            ImageVectorizationTaskCommand command,
            FailReason failReason,
            String message,
            Throwable cause) {

        Long imageId = command == null
                ? null
                : command.getImageId();

        if (imageAsset == null) {
            return buildFailedResult(
                    imageId,
                    failReason,
                    message);
        }

        VectorizationFailureContext context = buildFailureContext(
                imageAsset,
                command,
                failReason,
                message,
                cause);

        try {
            if (FailReason.FILE_NOT_FOUND
                    .equals(failReason)) {

                return failureHandler.handleFileMissing(
                        context);
            }

            if (retryPolicy.canRetry(context)) {
                return failureHandler
                        .handleRetryableFailure(context);
            }

            return failureHandler.handleDeadFailure(
                    context);
        } catch (RuntimeException handlerException) {
            /*
             * 原始异常已经记录。
             * 此处是失败处理器自身发生的第二个独立异常。
             */
            recordProcessorFailureSafely(
                    command,
                    imageAsset.getId(),
                    VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED,
                    handlerException);

            return buildFailedResult(
                    imageAsset.getId(),
                    failReason,
                    "vectorization failure handling failed");
        }
    }

    private VectorizationFailureContext buildFailureContext(
            ImageAssetDTO imageAsset,
            ImageVectorizationTaskCommand command,
            FailReason failReason,
            String message,
            Throwable cause) {

        return VectorizationFailureContext.builder()
                .imageId(imageAsset.getId())
                .traceId(command == null
                        ? null
                        : command.getTraceId())
                .requestId(command == null
                        ? null
                        : command.getRequestId())
                .failReason(failReason)
                .retryCount(imageAsset.getRetryCount())
                .maxRetryCount(
                        properties.getMaxRetryCount())
                .errorMessage(message)
                .cause(cause)
                .build();
    }

    private ErrorCode resolveErrorCode(
            Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {
            if (current instanceof BizException exception
                    && exception.getErrorCode() != null) {

                return exception.getErrorCode();
            }

            if (current instanceof SystemException exception
                    && exception.getErrorCode() != null) {

                return exception.getErrorCode();
            }


            current = current.getCause();
        }

        return null;
    }
    
    private FailReason mapFailReason(
            Throwable throwable) {

        ErrorCode errorCode = resolveErrorCode(throwable);

        if (errorCode == null) {
            return FailReason.MODEL_SERVICE_ERROR;
        }

        if (StorageErrorCode.FILE_NOT_FOUND.equals(
                errorCode)) {

            return FailReason.FILE_NOT_FOUND;
        }

        if (StorageErrorCode.FILE_READ_FAILED.equals(
                errorCode)
                || StorageErrorCode.STORAGE_UNAVAILABLE.equals(
                        errorCode)
                || VectorizationErrorCode.IMAGE_FILE_READ_FAILED
                        .equals(errorCode)) {

            return FailReason.TEMPORARY_STORAGE_ERROR;
        }

        if (StorageErrorCode.STORAGE_PATH_INVALID.equals(
                errorCode)) {

            return FailReason.STORAGE_PATH_INVALID;
        }

        if (ModelClientErrorCode.MODEL_SERVICE_TIMEOUT
                .equals(errorCode)) {

            return FailReason.MODEL_SERVICE_TIMEOUT;
        }

        if (ModelClientErrorCode.MODEL_SERVICE_UNAVAILABLE
                .equals(errorCode)
                || ModelClientErrorCode.MODEL_SERVICE_ERROR
                        .equals(errorCode)) {

            return FailReason.MODEL_SERVICE_ERROR;
        }

        if (ModelClientErrorCode.MODEL_DIM_MISMATCH
                .equals(errorCode)
                || VectorIndexErrorCode.VECTOR_DIM_MISMATCH
                        .equals(errorCode)) {

            return FailReason.VECTOR_DIM_MISMATCH;
        }

        if (VectorIndexErrorCode.VECTOR_UPSERT_FAILED
                .equals(errorCode)
                || VectorIndexErrorCode.VECTOR_INDEX_UNAVAILABLE
                        .equals(errorCode)) {

            return FailReason.VECTOR_DB_UPSERT_FAILED;
        }

        if (VectorizationErrorCode.VECTOR_READY_UPDATE_FAILED
                .equals(errorCode)) {

            return FailReason.READY_UPDATE_FAILED;
        }

        return FailReason.MODEL_SERVICE_ERROR;
    }


    private void recordProcessorFailureSafely(
            ImageVectorizationTaskCommand command,
            Long imageId,
            ErrorCode errorCode,
            Throwable throwable) {

        LogContext context = LogContext.builder()
                .requestId(command == null
                        ? null
                        : command.getRequestId())
                .traceId(command == null
                        ? null
                        : command.getTraceId())
                .bizId(imageId == null
                        ? null
                        : String.valueOf(imageId))
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION_PROCESS)
                .eventName(
                        LogEventName.SYSTEM_EXCEPTION_OCCURRED)
                .status(LogStatus.FAILED)
                .errorCode(errorCode == null
                        ? null
                        : String.valueOf(
                                errorCode.getCode()))
                .message(errorCode == null
                        ? VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED
                                .getMessage()
                        : errorCode.getMessage())
                .build();

        try {
            if (shouldRecordWithoutThrowable(throwable)) {
                logService.recordError(context);
            } else {
                logService.recordError(
                        context,
                        throwable);
            }
        } catch (RuntimeException loggingException) {
            if (throwable != null) {
                log.error(
                        "Vectorization processing failed",
                        throwable);
            }

            log.error(
                    "Failed to record vectorization error",
                    loggingException);
        }
    }

    /**
     * 显式失败结果转换出的 SystemException 没有底层 cause，
     * 不需要打印一份人为产生的包装异常堆栈。
     */
    private boolean shouldRecordWithoutThrowable(
            Throwable throwable) {

        if (throwable == null) {
            return true;
        }

        return throwable.getCause() == null
                && (throwable instanceof SystemException
                        || throwable instanceof BizException);
    }

    private void recordReadyCompensationEventSafely(
            Long imageId,
            ImageVectorizationTaskCommand command) {

        LogContext context = LogContext.builder()
                .requestId(command == null
                        ? null
                        : command.getRequestId())
                .traceId(command == null
                        ? null
                        : command.getTraceId())
                .bizId(imageId == null
                        ? null
                        : String.valueOf(imageId))
                .bizType(BIZ_TYPE)
                .module(MODULE)
                .operation(OPERATION_MARK_READY)
                .eventName(
                        LogEventName.VECTOR_READY_COMPENSATION_REQUIRED)
                .status(LogStatus.WAITING)
                .errorCode(String.valueOf(
                        VectorizationErrorCode.VECTOR_READY_UPDATE_FAILED
                                .getCode()))
                .message(
                        "vector upsert succeeded but READY update failed")
                .build();

        try {
            logService.recordEvent(context);
        } catch (RuntimeException loggingException) {
            log.error(
                    "Failed to record READY compensation event",
                    loggingException);
        }
    }

    private void recordTaskMetrics(
            String source,
            TaskOutcome outcome,
            long startNanos) {

        MetricTags taskTags = MetricTags.builder()
                .tag(MetricTag.SOURCE, source)
                .tag(
                        MetricTag.RESULT,
                        outcome.getResult())
                .tag(
                        MetricTag.REASON,
                        outcome.getReason())
                .build();

        metricRecorder.increment(
                MetricName.VECTORIZATION_TASKS,
                taskTags);

        metricRecorder.recordTimer(
                MetricName.VECTORIZATION_DURATION,
                elapsedMillis(startNanos),
                MetricTags.builder()
                        .tag(MetricTag.SOURCE, source)
                        .tag(
                                MetricTag.RESULT,
                                outcome.getResult())
                        .build());
    }

    private String resolveSource(
            ImageVectorizationTaskCommand command) {

        if (command == null
                || command.getSource() == null
                || command.getSource().isBlank()) {

            return SOURCE_UNKNOWN;
        }

        String source = command.getSource()
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (source) {
            case SOURCE_IMAGE_UPLOAD -> SOURCE_IMAGE_UPLOAD;
            case SOURCE_SCANNER -> SOURCE_SCANNER;
            case SOURCE_MANUAL -> SOURCE_MANUAL;
            default -> SOURCE_UNKNOWN;
        };
    }

    private String normalizeReason(
            FailReason failReason) {

        return failReason == null
                ? REASON_UNKNOWN
                : failReason.name()
                        .toLowerCase(Locale.ROOT);
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

    private ImageVectorizationResult buildSuccessResult(
            Long imageId) {

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(true)
                .skipped(false)
                .vectorStatus(VectorStatus.READY)
                .message("image vectorization success")
                .build();
    }

    private ImageVectorizationResult buildAssetNotFoundResult(
            Long imageId) {

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(true)
                .message(
                        VectorizationErrorCode.IMAGE_ASSET_NOT_FOUND
                                .getMessage())
                .build();
    }

    private ImageVectorizationResult buildSkippedResult(
            ImageAssetDTO imageAsset) {

        return ImageVectorizationResult.builder()
                .imageId(imageAsset.getId())
                .success(false)
                .skipped(true)
                .vectorStatus(
                        imageAsset.getVectorStatus())
                .message(
                        "image asset status not allowed for vectorization")
                .build();
    }

    private ImageVectorizationResult buildFailedResult(
            Long imageId,
            FailReason failReason,
            String message) {

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.FAILED)
                .failReason(failReason)
                .message(message)
                .build();
    }

    private static final class TaskOutcome {

        private String result = RESULT_FAILED;

        private String reason = REASON_UNKNOWN;

        void success() {
            result = RESULT_SUCCESS;
            reason = REASON_NONE;
        }

        void skipped(String skippedReason) {
            result = RESULT_SKIPPED;
            reason = skippedReason == null
                    ? REASON_UNKNOWN
                    : skippedReason;
        }

        void failed(String failureReason) {
            result = RESULT_FAILED;
            reason = failureReason == null
                    ? REASON_UNKNOWN
                    : failureReason;
        }

        void fromFailureResult(
                ImageVectorizationResult failureResult,
                FailReason failReason) {

            reason = failReason == null
                    ? REASON_UNKNOWN
                    : failReason.name()
                            .toLowerCase(Locale.ROOT);

            if (failureResult != null
                    && VectorStatus.PENDING.equals(
                            failureResult
                                    .getVectorStatus())) {

                result = RESULT_RETRYING;
                return;
            }

            result = RESULT_FAILED;
        }

        String getResult() {
            return result;
        }

        String getReason() {
            return reason;
        }
    }
}