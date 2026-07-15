package com.everypicfound.vectorization.api;

public interface VectorizationTaskPublisher {

    VectorizationPublishResult publish(ImageVectorizationTaskCommand command);
}
