package com.everypicfound.vectorization.domain.failure;

import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;

public interface VectorizationFailureHandler {

    ImageVectorizationResult handleFileMissing(VectorizationFailureContext context);

    ImageVectorizationResult handleRetryableFailure(VectorizationFailureContext context);

    ImageVectorizationResult handleDeadFailure(VectorizationFailureContext context);
    
}
