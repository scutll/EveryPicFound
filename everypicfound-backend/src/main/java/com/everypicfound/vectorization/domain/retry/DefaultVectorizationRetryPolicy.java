package com.everypicfound.vectorization.domain.retry;

import java.util.EnumSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.vectorization.config.VectorizationProperties;
import com.everypicfound.vectorization.domain.model.VectorizationFailureContext;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultVectorizationRetryPolicy implements VectorizationRetryPolicy {
    
    private static final Set<FailReason> RETRYABLE_REASONS = EnumSet.of(
            FailReason.IMAGE_DECODE_ERROR,
            FailReason.MODEL_SERVICE_TIMEOUT,
            FailReason.MODEL_SERVICE_ERROR,
            FailReason.VECTOR_DB_UPSERT_FAILED,
            FailReason.READY_UPDATE_FAILED,
            FailReason.PROCESSING_TIMEOUT
    );

    private final VectorizationProperties properties;

    @Override
    public boolean canRetry(VectorizationFailureContext context) {
        if (context == null) {
            return false;
        }

        Integer maxRetryCount = context.getMaxRetryCount() == null
                ? properties.getMaxRetryCount()
                : context.getMaxRetryCount();

        return canRetry(context.getFailReason(), context.getRetryCount(), maxRetryCount);
    }
    
    @Override
    public boolean canRetry(FailReason failReason, Integer retryCount) {
        return canRetry(failReason, retryCount, properties.getMaxRetryCount());
    }

    @Override
    public boolean isRetryableFailReason(FailReason failReason) {
        return failReason != null && RETRYABLE_REASONS.contains(failReason);
    }

    private boolean canRetry(FailReason failReason, Integer retryCount, Integer maxRetryCount) {
        if (!isRetryableFailReason(failReason)) {
            return false;
        }

        int currentRetryCount = retryCount == null ? 0 : retryCount;
        int allowedRetryCount = maxRetryCount == null ? 0 : maxRetryCount;


        return currentRetryCount < allowedRetryCount;
    }

}
