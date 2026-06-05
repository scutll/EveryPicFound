package com.everypicfound.vectorization.domain.query;

import java.util.List;

import com.everypicfound.search.domain.enums.SearchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueryEmbedding {
    
    private SearchType searchType;

    private List<Float> embedding;

    private Integer dim;

    private String modelName;
}
