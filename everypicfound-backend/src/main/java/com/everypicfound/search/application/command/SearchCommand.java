package com.everypicfound.search.application.command;

import java.io.InputStream;

import com.everypicfound.search.domain.enums.SearchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchCommand {
    
    private SearchType searchType;

    // 图片部分
    private InputStream queryImage;

    private String queryImageOriginalFileName;

    private Long queryImageFileSize;

    private String queryImageMimeType;

    //文字部分
    private String queryText;

    private Integer topK;

    private String traceId;

    private String requestId;

    private Integer topK;

}
