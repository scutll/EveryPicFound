package com.everypicfound.vectorization.domain.query;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.error.SearchErrorCode;

@Component
public class WeightedHybridFusionStrategy implements HybridFusionStrategy {
    
    @Override
    public HybridFusionResult fuse(HybridFusionRequest request) {
        validate(request);

        List<Float> imageEmbedding = request.getImageEmbedding();
        List<Float> textEmbedding = request.getTextEmbedding();


        float imageWeight = request.getImageWeight();
        float textWeight = request.getTextWeight();
        float weightSum = imageWeight + textWeight;

        int embedding_size = imageEmbedding.size();

        List<Float> fused = new ArrayList<>(embedding_size);


        for (int i = 0; i < embedding_size; i++) {
            float value = imageEmbedding.get(i) * imageWeight / weightSum
                    + textEmbedding.get(i) * textWeight / weightSum;
            fused.add(value);
        }


        if (Boolean.TRUE.equals(request.getNormalizeEnabled())) {
            fused = normalize(fused);
        }

        return HybridFusionResult.builder()
                .hybridEmbedding(fused)
                .dim(fused.size())
                .build();
    }
    

    private void validate(HybridFusionRequest request) {
        if (request == null
                || request.getImageEmbedding() == null
                || request.getTextEmbedding() == null
                || request.getImageEmbedding().isEmpty()
                || request.getTextEmbedding().isEmpty()
                || request.getImageEmbedding().size() != request.getTextEmbedding().size()
                || request.getImageWeight() == null
                || request.getTextWeight() == null
                || request.getImageWeight() < 0
                || request.getTextWeight() < 0
                || request.getImageWeight() + request.getTextWeight() <= 0) {
            throw new BizException(SearchErrorCode.QUERY_VECTORIZATION_FAILED);
        }
    }

    private List<Float> normalize(List<Float> vector) {
        double sum = 0D;
        for (Float value : vector) {
            if (vector != null) {
                sum += value * value;
            }
        }

        double norm = Math.sqrt(sum);
        if (norm == 0D) {
            return vector;
        }

        List<Float> normalized = new ArrayList<>(vector.size());
        for (Float value : vector) {
            normalized.add((float) (value / norm));
        }

        return normalized;
    }
    
}
