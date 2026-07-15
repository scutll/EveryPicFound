package com.everypicfound.search.domain.collection;

import com.everypicfound.vectorindex.collection.DistanceMetric;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchCollectionContext {

    private String collectionName;

    private String modelName;

    private Integer vectorDim;

    private String vectorVersion;

    private DistanceMetric distanceMetric;
}