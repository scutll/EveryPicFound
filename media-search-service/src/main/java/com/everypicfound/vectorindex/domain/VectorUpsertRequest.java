package com.everypicfound.vectorindex.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorUpsertRequest {
    
    private String collectionName;

    private Long vectorId;

    private List<Float> embedding;

    private VectorPayload payload;

}
