package com.everypicfound.search.application.pipeline;

import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.command.SearchCommand;

public interface SearchPipeline {
    SearchResponse execute(SearchCommand SearchCommand);    
}
