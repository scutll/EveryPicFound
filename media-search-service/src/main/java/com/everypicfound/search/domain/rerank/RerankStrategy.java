package com.everypicfound.search.domain.rerank;

public interface RerankStrategy {
    
    RerankResult rerank(RerankContext context);
}
