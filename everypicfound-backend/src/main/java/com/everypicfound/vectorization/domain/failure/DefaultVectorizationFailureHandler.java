package com.everypicfound.vectorization.domain.failure;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.imageasset.domain.service.ImageAssetStatusService;
import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultVectorizationFailureHandler implements VectorizationFailureHandler {
    
    private final ImageAssetStatusService imageAssetStatusService;

    @Override
    public ImageVectorizationResult handleFileMissing(VectorizationFailureContext context) {
        Long imageId = getImageId(context);
        FailReason failReason = getFailReason(context);

        imageAssetStatusService.increaseRetryCount(imageId);
        imageAssetStatusService.markVectorPending(imageId);

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .vectorStatus(VectorStatus.PENDING)
                .failReason(failReason)
                .message(context.getErrorMessage())
                .build();

    }

    @Override
    public ImageVectorizationResult handleRetryableFailure(VectorizationFailureContext context) {
        Long imageId = getImageId(context);
        FailReason failReason = getFailReason(context);

        imageAssetStatusService.increaseRetryCount(imageId);
        imageAssetStatusService.markVectorPending(imageId);

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.PENDING)
                .failReason(failReason)
                .message(context.getErrorMessage())
                .build();
    }

    @Override
    public ImageVectorizationResult handleDeadFailure(VectorizationFailureContext context) {
        Long imageId = getImageId(context);
        FailReason failReason = getFailReason(context);

        imageAssetStatusService.markVectorFailed(imageId, failReason);

        return ImageVectorizationResult.builder()
                .imageId(imageId)
                .success(false)
                .skipped(false)
                .vectorStatus(VectorStatus.FAILED)
                .failReason(failReason)
                .message(context.getErrorMessage())
                .build();
    }
    



    private Long getImageId(VectorizationFailureContext context) {
        if (context == null || context.getImageId() == null) {
            throw new BizException(VectorizationErrorCode.VECTORIZATION_PROCESS_FAILED);
        }

        return context.getImageId();
    }
    
    private FailReason getFailReason(VectorizationFailureContext context) {
        if (context.getFailReason() == null) {
            return FailReason.MODEL_SERVICE_ERROR;
        }
        return context.getFailReason();
    }
    
}
