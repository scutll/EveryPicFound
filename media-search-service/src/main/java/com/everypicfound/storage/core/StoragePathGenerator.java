package com.everypicfound.storage.core;
public interface StoragePathGenerator {

    // 生成统一 storagePath。
    String generatePath(StoragePathContext context);
}
