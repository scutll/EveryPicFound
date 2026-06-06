package com.everypicfound.search.domain.assembler;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.context.SearchResultItem;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultSearchResultAssembler implements SearchResultAssembler {
    
    @Override
    public SearchResponse assemble(SearchAssemblerContext context) {
        if (context == null) {
            return emptyResponse();
        }

        List<SearchResultItem> items = context.getItems() == null
                ? Collections.emptyList()
                : context.getItems();

        return SearchResponse.builder()
                .searchType(context.getSearchType())
                .total(items.size())
                .items(items)
                .costMs(context.getCostMs())
                .build();
    }


    private SearchResponse emptyResponse() {
        return SearchResponse.builder()
                .total(0)
                .items(Collections.emptyList())
                .costMs(0L)
                .build();
    }
}
