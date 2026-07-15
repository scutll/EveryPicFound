package com.everypicfound.search.domain.assembler;

import java.util.List;

import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.domain.enums.SearchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchAssemblerContext {
    
    private SearchType searchType;

    private List<SearchResultItem> items;

    private Integer topK;

    private Long costMs;

    private Integer totalRecallCount;

    private Integer orphanVectorCount;

    private Integer invalidImageCount;
}
