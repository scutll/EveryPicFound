package com.everypicfound.search.application.service;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.pipeline.SearchPipeline;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultSearchApplicationService implements SearchApplicationService {

    private final SearchPipeline searchPipeline;

    @Override
    public SearchResponse search(SearchCommand command) {
        return searchPipeline.execute(command);
    }
}
