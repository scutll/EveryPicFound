package com.everypicfound.search.domain.overfetch;

import com.everypicfound.search.domain.enums.SearchType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OverFetchContext {

    private SearchType searchType;

    private Integer topK;

    private Integer maxTopN;

    private Integer overFetchRatio;
}