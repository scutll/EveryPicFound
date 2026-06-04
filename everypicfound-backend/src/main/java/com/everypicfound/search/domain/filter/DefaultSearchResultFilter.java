package com.everypicfound.search.domain.filter;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.vectorindex.domain.VectorSearchItem;

@Component
public class DefaultSearchResultFilter implements SearchResultFilter {
    
    @Override
    public SearchFilterResult filter(SearchFilterContext context) {
        if (context == null || context.getVectorItems() == null || context.getVectorItems().isEmpty()) {
            return emptyResult();
        }

        Map<Long, ImageAssetDTO> imageAssetMap = buildImageAssetMap(context.getImageAssets());

        int orphanVectorCount = 0;
        int invalidImageCount = 0;
        List<SearchResultItem> resultItems = new java.util.ArrayList<>();

        for (VectorSearchItem vectorItem : context.getVectorItems()) {
            if (vectorItem == null || vectorItem.getVectorId() == null) {
                orphanVectorCount++;
                continue;
            }

            ImageAssetDTO imageAsset = imageAssetMap.get(vectorItem.getVectorId());
            if (imageAsset == null) {
                orphanVectorCount++;
                continue;
            }

            if (!isSearchable(imageAsset)) {
                invalidImageCount++;
                continue;
            }

            resultItems.add(buildSearchResultItem(vectorItem, imageAsset));
        }
        
        return SearchFilterResult.builder()
                .items(resultItems)
                .orphanVectorCount(orphanVectorCount)
                .invalidImageCount(invalidImageCount)
                .build();


    }
    
    private Map<Long, ImageAssetDTO> buildImageAssetMap(List<ImageAssetDTO> imageAssets) {
        if (imageAssets == null || imageAssets.isEmpty()) {
            return Collections.emptyMap();
        }

        return imageAssets.stream()
                .filter(Objects::nonNull)
                .filter(imageAsset -> imageAsset.getId() != null)
                .collect(Collectors.toMap(ImageAssetDTO::getId, imageAsset -> imageAsset, (first, second) -> first));
    }
    
    private boolean isSearchable(ImageAssetDTO imageAsset) {
        return ImageStatus.NORMAL.equals(imageAsset.getImageStatus())
                && VectorStatus.READY.equals(imageAsset.getVectorStatus());
    }


    private SearchResultItem buildSearchResultItem(VectorSearchItem vectorItem, ImageAssetDTO imageAsset) {
        return SearchResultItem.builder()
                .imageId(imageAsset.getId())
                .imageUrl(imageAsset.getImageUrl())
                .fileName(imageAsset.getFileName())
                .originalFileName(imageAsset.getOriginalFileName())
                .score(vectorItem.getScore())
                .width(imageAsset.getWidth())
                .height(imageAsset.getHeight())
                .mimeType(imageAsset.getMimeType())
                .build();

    }
    
    private SearchFilterResult emptyResult() {
        return SearchFilterResult.builder()
                .items(Collections.emptyList())
                .orphanVectorCount(0)
                .invalidImageCount(0)
                .build();
    }
    
}
