package com.everypicfound.search.application.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultItem {
    
    private Long imageId;

    private String imageUrl;

    private String fileName;

    private String originalFileName;

    private Float score;

    private Integer width;

    private Integer height;

    private String mimeType;
}
