package com.everypicfound.search.domain.overfetch;

public interface OverFetchStrategy {

    Integer calculateTopN(OverFetchContext context);
}