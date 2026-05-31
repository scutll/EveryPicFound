package com.everypicfound.vectorindex.collection;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VectorCollectionHealthResult {

    
    private Boolean success;

    private String collectionName;

    private Boolean exists;

    private Boolean created;

    private Boolean vectorDimMatched;

    private String errorCode;

    private ErrorCode message;

    private Long costMs;
}
