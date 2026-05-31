package com.everypicfound.vectorindex.collection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VectorCollectionConfig {
    
    private String collectionName;

    private String modelName;

    private Integer vectorDim;

    private String vectorVersion;

    private DistanceMetric distanceMetric;
}
