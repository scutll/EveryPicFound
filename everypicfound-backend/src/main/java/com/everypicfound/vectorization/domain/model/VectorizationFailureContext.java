package com.everypicfound.vectorization.domain.model;

import com.everypicfound.imageasset.domain.enums.FailReason;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorizationFailureContext {
    
    private Long imageId;

    private String traceId;

    private String requestId;

    private FailReason failReason;

    private Integer retryCount;

    private Integer maxRetryCount;

    private String errorMessage;

    private Throwable cause;
}
