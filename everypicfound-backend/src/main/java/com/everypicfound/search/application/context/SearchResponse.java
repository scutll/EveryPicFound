package com.everypicfound.search.application.context;

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
public class SearchResponse {
    
    private SearchType searchType;

    private Integer total;

    private List<SearchResultItem> items;

    private Long costMs;
}
