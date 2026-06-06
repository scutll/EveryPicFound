package com.everypicfound.search.domain.rerank;

import java.util.List;

import com.everypicfound.search.application.context.SearchResultItem;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RerankResult {
    
    private List<SearchResultItem> items;

    private Boolean rerankApplied;
}
