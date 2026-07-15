package com.everypicfound.search.domain.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.vectorindex.domain.VectorSearchItem;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchFilterContext {
    
    private List<VectorSearchItem> vectorItems;

    private List<ImageAssetDTO> imageAssets;

    private SearchType searchType;
}
