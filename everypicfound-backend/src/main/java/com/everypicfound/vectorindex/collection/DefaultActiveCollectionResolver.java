package com.everypicfound.vectorindex.collection;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultActiveCollectionResolver implements ActiveCollectionResolver {
    
    private final VectorCollectionConfigProvider configProvider;

    @Override
    public VectorCollectionConfig resolveActiveCollection() {
        return configProvider.getActiveConfig();
    }
}
