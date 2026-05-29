package com.everypicfound.vectorization.api;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorizationPublishResult {

    private Boolean success;

    private Long imageId;

    private ErrorCode errorCode;

    private String message;
}