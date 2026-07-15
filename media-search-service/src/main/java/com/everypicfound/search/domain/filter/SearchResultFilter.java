package com.everypicfound.search.domain.filter;

public interface SearchResultFilter {
    
    SearchFilterResult filter(SearchFilterContext context);
}
