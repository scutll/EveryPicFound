package com.everypicfound.search.domain.rerank;

import java.util.List;

import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.vectorization.domain.query.QueryEmbedding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RerankContext {
    
    private List<SearchResultItem> items;

    private SearchType searchType;

    private QueryEmbedding queryEmbedding;

}
