package com.everypicfound.vectorindex.domain;

import java.util.List;

import com.everypicfound.common.exception.ErrorCode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchResult {
    
    private List<VectorSearchItem> items;

    private String collectionName;

    private Integer topN;

    private Long costMs;

    private ErrorCode errorCode;

    private String message;

}
