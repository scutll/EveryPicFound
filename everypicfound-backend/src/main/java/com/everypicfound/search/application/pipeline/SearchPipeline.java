package com.everypicfound.search.application.pipeline;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;

public interface SearchPipeline {

    SearchResponse execute(SearchCommand command);//执行搜索逻辑
}
