package com.everypicfound.vectorization.infrastructure.publisher;

import org.springframework.stereotype.Service;

import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;

@Service
public class ThreadPoolVectorizationTaskPublisher implements VectorizationTaskPublisher {
    
    @Override
    public VectorizationPublishResult publish(ImageVectorizationTaskCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
