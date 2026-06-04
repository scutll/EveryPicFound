package com.everypicfound.search.domain.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.everypicfound.search.application.context.SearchResultItem;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchFilterResult {
    
    private List<SearchResultItem> items;

    private Integer orphanVectorCount;

    private Integer invalidImageCount;

}
