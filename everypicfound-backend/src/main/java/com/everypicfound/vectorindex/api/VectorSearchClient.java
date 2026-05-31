package com.everypicfound.vectorindex.api;

import com.everypicfound.vectorindex.domain.VectorSearchRequest;
import com.everypicfound.vectorindex.domain.VectorSearchResult;

public interface VectorSearchClient {
    
    VectorSearchResult search(VectorSearchRequest request);
}
