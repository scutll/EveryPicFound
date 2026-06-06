package com.everypicfound.search.application.service;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;

public interface SearchApplicationService {

    SearchResponse search(SearchCommand command);
}
