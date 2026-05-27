package com.everypicfound.imageasset.domain.generator;

import org.springframework.stereotype.Component;

@Component
public class DefaultImageFileNameGenerator implements ImageFileNameGenerator {
    
    @Override
    public String generateFileName(Long imageId, String fileExt) {
        throw new UnsupportedOperationException("TODO");
    }
}
