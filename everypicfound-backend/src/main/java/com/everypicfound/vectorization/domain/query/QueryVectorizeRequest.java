package com.everypicfound.vectorization.domain.query;

import java.io.InputStream;

import com.everypicfound.search.domain.enums.SearchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueryVectorizeRequest {

    private SearchType searchType;

    private InputStream queryImage;

    private String queryImageOriginalFileName;

    private Long queryImageFileSize;

    private String queryImageMimeType;

    private String queryText;

    private String traceId;

    private String requestId;
}