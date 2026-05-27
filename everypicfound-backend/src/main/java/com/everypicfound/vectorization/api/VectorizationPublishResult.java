package com.everypicfound.vectorization.api;

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

    private String errorCode;

    private String message;
}