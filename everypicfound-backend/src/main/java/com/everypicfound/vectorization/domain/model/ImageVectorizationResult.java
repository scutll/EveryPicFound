package com.everypicfound.vectorization.domain.model;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.VectorStatus;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImageVectorizationResult {
    

    private Long imageId;

    private Boolean success;

    private Boolean skipped;

    private VectorStatus vectorStatus;

    private FailReason failReason;

    private String message;
}
