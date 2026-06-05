package com.everypicfound.vectorization.domain.query;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridFusionRequest {

    private List<Float> imageEmbedding;

    private List<Float> textEmbedding;

    private Float imageWeight;

    private Float textWeight;

    private Boolean normalizeEnabled;
}