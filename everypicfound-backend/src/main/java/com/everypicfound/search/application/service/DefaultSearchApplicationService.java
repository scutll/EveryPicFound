package com.everypicfound.search.application.service;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class DefaultSearchApplicationService implements SearchApplicationService {

    @Override
    public SearchResponse search(SearchCommand command) {
        return SearchResponse.builder()
                .searchType(command.getSearchType())
                .total(0)
                .items(Collections.emptyList())
                .costMs(0L)
                .build();
    }
}
