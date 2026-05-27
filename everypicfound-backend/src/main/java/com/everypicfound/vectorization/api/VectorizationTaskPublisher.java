package com.everypicfound.vectorization.api;

public interface VectorizationTaskPublisher {

    // 发布向量化任务。
    void publish(Long imageId);
}
