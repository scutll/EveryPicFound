package com.everypicfound.search.domain.assembler;

import com.everypicfound.search.application.context.SearchResponse;

public interface SearchResultAssembler {
    
    SearchResponse assemble(SearchAssemblerContext context);
}
