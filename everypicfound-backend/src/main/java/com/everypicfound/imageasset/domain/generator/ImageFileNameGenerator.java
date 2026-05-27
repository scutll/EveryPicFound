package com.everypicfound.imageasset.domain.generator;

public interface ImageFileNameGenerator {

    // 生成系统文件名，例如 100001.jpg。
    String generateFileName(Long imageId, String fileExt);
}
