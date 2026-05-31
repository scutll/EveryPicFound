package com.everypicfound.vectorindex.collection;


public interface VectorCollectionConfigProvider {

    VectorCollectionConfig getActiveConfig();

    VectorCollectionConfig getConfig(String collectionName);

    void validateConfig(VectorCollectionConfig config);
    
}
