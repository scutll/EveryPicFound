package com.everypicfound.vectorization.domain.fusion;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HybridFusionResult {

    private List<Float> hybridEmbedding;

    private Integer dim;
}