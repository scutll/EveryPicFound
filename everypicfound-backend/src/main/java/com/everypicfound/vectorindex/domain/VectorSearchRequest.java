package com.everypicfound.vectorindex.domain;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorSearchRequest {

    private String collectionName;

    private List<Float> queryEmbedding;

    private Integer topN;
}