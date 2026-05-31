package com.everypicfound.vectorindex.domain;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VectorOperationResult {
    
    private Boolean success;

    private Long vectorId;

    private Boolean exists;

    private Long costMs;

    private ErrorCode errorCode;

    private String message;
}
