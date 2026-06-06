package com.everypicfound.search.domain.rerank;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.everypicfound.search.application.context.SearchResultItem;

@Component
public class NoOpRerankStrategy implements RerankStrategy{
    

    @Override
    public RerankResult rerank(RerankContext context) {
        List<SearchResultItem> items = context == null || context.getItems() == null?
            Collections.emptyList()
                : context.getItems();

        return RerankResult.builder()
                .items(items)
                .rerankApplied(false)
                .build();
    }
}
