package com.everypicfound.vectorindex.infrastructure.client;

import org.springframework.stereotype.Service;

import com.everypicfound.vectorindex.api.VectorSearchClient;
import com.everypicfound.vectorindex.domain.VectorSearchRequest;
import com.everypicfound.vectorindex.domain.VectorSearchResult;

@Service
public class DefaultVectorSearchClient implements VectorSearchClient {
    
    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        throw new UnsupportedOperationException("TODO");
    }
}
