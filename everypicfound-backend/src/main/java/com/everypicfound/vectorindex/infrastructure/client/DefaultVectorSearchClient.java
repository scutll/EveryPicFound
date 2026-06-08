package com.everypicfound.vectorindex.infrastructure.client;


import com.everypicfound.vectorindex.api.VectorSearchClient;
import com.everypicfound.vectorindex.domain.VectorSearchRequest;
import com.everypicfound.vectorindex.domain.VectorSearchResult;


public class DefaultVectorSearchClient implements VectorSearchClient {
    
    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        throw new UnsupportedOperationException("TODO");
    }
}
