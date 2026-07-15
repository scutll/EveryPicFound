package com.everypicfound.vectorindex.collection.health;

public interface VectorCollectionHealthChecker {
    
    void check();

    boolean checkCollectionExists(String collectName);

    void ensureActiveCollectionReady();
}
