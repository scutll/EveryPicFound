package com.everypicfound.vectorization.domain.query;

import com.everypicfound.search.domain.enums.SearchType;

public interface QueryVectorizer {
    
    SearchType supportType();

    QueryEmbedding vectorize(QueryVectorizeRequest request);

    
}
