package com.everypicfound.imageasset.domain.generator;

public interface ImageFileNameGenerator {

    String generateFileName(Long imageId, String fileExt);
}