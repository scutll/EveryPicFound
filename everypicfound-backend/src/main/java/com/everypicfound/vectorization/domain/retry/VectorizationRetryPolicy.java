package com.everypicfound.vectorization.domain.retry;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;

public interface VectorizationRetryPolicy {
    
    boolean canRetry(VectorizationFailureContext context);

    boolean canRetry(FailReason failReason, Integer retryCount);

    boolean isRetryableFailReason(FailReason failReason);
    
}
