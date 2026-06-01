package com.everypicfound.vectorization.application.processor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.BizException;
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

@Service
@RequiredArgsConstructor
public class DefaultImageVectorizationProcessor implements ImageVectorizationProcessor {
    
    private final ImageAssetQueryService imageAssetQueryService;

    private final ImageAssetStatusService imageAssetStatusService;

    private final FileStorageService fileStorageService;

    private final ModelVectorizationClient modelVectorizationClient;

    private final VectorIndexClient vectorIndexClient;

    private final ActiveCollectionResolver activeCollectionResolver;

    private final VectorizationRetryPolicy retryPolicy;

    private final VectorizationFailureHandler failureHandler;

    private final VectorizationProperties properties;
    

    @Override
    public ImageVectorizationResult process(ImageVectorizationTaskCommand command) {
        if (command == null || command.getImageId() == null) {
            return failed(null, FailReason.MODEL_SERVICE_ERROR, "vectorization command invalid");
        }

        Long imageId = command.getImageId();

        try{

            ImageAssetDTO imageAsset = imageAssetQueryService.getById(imageId);
            if(imageAsset == null){
                return failed(imageId, FailReason.MODEL_SERVICE_ERROR, "image asset not found");
            }

            if(!canStartVectorization(imageAsset)){
                return skipped(imageAsset);
            }

            imageAssetStatusService.markVectorProcessing(imageId);

            if(!fileStorageService.exists(imageAsset.getStoragePath())){
                return failureHandler.handleFileMissing(buildFailureContext(
                        imageAsset,
                        command,
                        FailReason.FILE_NOT_FOUND,
                        "image file not found",
                        null
                ));
            }

            StorageResource storageResource = readStorageResource(imageAsset, command);
            VectorCollectionConfig collectionConfig = activeCollectionResolver.resolveActiveCollection();
            VectorizeResult result = vectorizeImage(imageAsset, storageResource,collectionConfig, command);

            if(!isDimMatched(result, collectionConfig)){
                return handleFailure(imageAsset, command, FailReason.VECTOR_DIM_MISMATCH,
                        "vector dimension mismatch", null); 
            }

            VectorOperationResult upsertResult = upsertVector(imageAsset, result, collectionConfig);
            if(upsertResult == null || !Boolean.TRUE.equals(upsertResult.getSuccess())){
                return handleFailure(imageAsset, command, FailReason.VECTOR_DB_UPSERT_FAILED,
                        "vector upsert failed", null);
            }

            imageAssetStatusService.markVectorReady(imageId);

            return ImageVectorizationResult.builder()
                    .imageId(imageId)
                    .success(true)
                    .skipped(false)
                    .vectorStatus(VectorStatus.READY)
                    .message("image vectorization success")
                    .build();
        } catch (BizException e){
            return handleFailure(imageId, command, mapFailReason(e), e.getMessage(), e);
        } catch (Exception e) {
            return handleFailure(imageId, command, FailReason.MODEL_SERVICE_ERROR, e.getMessage(), e);
        }
    }




    private boolean canStartVectorization(ImageAssetDTO imageAsset) {
        return ImageStatus.NORMAL.equals(imageAsset.getImageStatus())
                && VectorStatus.PENDING.equals(imageAsset.getVectorStatus());
    }

    private ImageVectorizationResult skipped(ImageAssetDTO imageAsset) {
        return ImageVectorizationResult.builder()
                .imageId(imageAsset.getId())
                .success(false)
                .skipped(true)
                .vectorStatus(imageAsset.getVectorStatus())
                .message("image asset status not allowed for vectorization")
                .build();
    }

    private StorageResource readStorageResource(ImageAssetDTO imageAsset, ImageVectorizationTaskCommand command) {
        try {
            return fileStorageService.read(imageAsset.getStoragePath());
        } catch (Exception e) {
            throw new BizException(VectorizationErrorCode.IMAGE_FILE_READ_FAILED, e);
        }
    }
    
    private VectorizeResult vectorizeImage(ImageAssetDTO imageAsset,
            StorageResource storageResource,
            VectorCollectionConfig collectionConfig,
            ImageVectorizationTaskCommand command) {

        ImageVectorizeRequest request = ImageVectorizeRequest.builder()
                .imageInputType(ImageInputType.MULTIPART)
                .imageId(imageAsset.getId())
                .storagePath(imageAsset.getStoragePath())
                .inputStream(storageResource.getInputStream())
                .originalFileName(imageAsset.getOriginalFileName())
                .fileSize(storageResource.getFileSize())
                .mimeType(storageResource.getMimeType())
                .modelName(collectionConfig.getModelName())
                .traceId(command.getTraceId())
                .requestId(command.getRequestId())
                .build();

        return modelVectorizationClient.vectorizeImage(request);

    }

    private boolean isDimMatched(VectorizeResult result, VectorCollectionConfig collectionConfig) {
        if (result == null || result.getDim() == null || collectionConfig == null) {
            return false;
        }

        return result.getDim().equals(collectionConfig.getVectorDim());
    }

    private VectorOperationResult upsertVector(ImageAssetDTO imageAsset,
                                               VectorizeResult vectorizeResult,
            VectorCollectionConfig collectionConfig) {
        VectorUpsertRequest request = VectorUpsertRequest.builder()
                .collectionName(collectionConfig.getCollectionName())
                .vectorId(imageAsset.getId())
                .embedding(vectorizeResult.getEmbedding())
                .payload(VectorPayload.builder()
                        .createdTime(LocalDateTime.now())
                        .build())
                .build();

        return vectorIndexClient.upsert(request);
    }
    
    private ImageVectorizationResult handleFailure(Long imageId, ImageVectorizationTaskCommand command,
            FailReason failReason, String message, Throwable cause) {
        ImageAssetDTO imageAsset = imageId == null ? null : imageAssetQueryService.getById(imageId);
        if (imageAsset == null) {
            return failed(imageId, failReason, message);
        }

        return handleFailure(imageAsset, command, failReason, message, cause);
    }

    private ImageVectorizationResult handleFailure(ImageAssetDTO imageAsset,
                                                   ImageVectorizationTaskCommand command,
                                                   FailReason failReason,
                                                   String message,
            Throwable cause) {

        VectorizationFailureContext context = buildFailureContext(
                imageAsset,
                command,
                failReason,
                message,
                cause);

        if (FailReason.FILE_NOT_FOUND.equals(failReason)) {
            return failureHandler.handleFileMissing(context);
        }

        if (retryPolicy.canRetry(context)) {
            return failureHandler.handleRetryableFailure(context);
        }

        return failureHandler.handleDeadFailure(context);

    }
    
    private VectorizationFailureContext buildFailureContext(ImageAssetDTO imageAsset,
            ImageVectorizationTaskCommand command,
            FailReason failReason,
            String message,
            Throwable cause) {
        return VectorizationFailureContext.builder()
                .imageId(imageAsset.getId())
                .traceId(command.getTraceId())
                .requestId(command.getRequestId())
                .failReason(failReason)
                .retryCount(imageAsset.getRetryCount())
                .maxRetryCount(properties.getMaxRetryCount())
                .errorMessage(message)
                .cause(cause)
                .build();
    }

    private FailReason mapFailReason(BizException e) {
        if (ModelClientErrorCode.MODEL_SERVICE_TIMEOUT.equals(e.getErrorCode())) {
            return FailReason.MODEL_SERVICE_TIMEOUT;
        }

        if (ModelClientErrorCode.MODEL_DIM_MISMATCH.equals(e.getErrorCode())
                || VectorIndexErrorCode.VECTOR_DIM_MISMATCH.equals(e.getErrorCode())) {
            return FailReason.VECTOR_DIM_MISMATCH;
        }

        if (VectorIndexErrorCode.VECTOR_UPSERT_FAILED.equals(e.getErrorCode())
                || VectorIndexErrorCode.VECTOR_INDEX_UNAVAILABLE.equals(e.getErrorCode())) {
            return FailReason.VECTOR_DB_UPSERT_FAILED;
        }

        if (VectorizationErrorCode.IMAGE_FILE_READ_FAILED.equals(e.getErrorCode())) {
            return FailReason.FILE_NOT_FOUND;
        }

        return FailReason.MODEL_SERVICE_ERROR;
    }




    private ImageVectorizationResult failed(Long imageId, FailReason failReason, String message) {
        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.FAILED)
                .failReason(failReason)
                .message(message)
                .build();
    }

}
