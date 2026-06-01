package com.everypicfound.vectorization.application.processor;

import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.domain.model.ImageVectorizationResult;

public interface ImageVectorizationProcessor {
    
    ImageVectorizationResult process(ImageVectorizationTaskCommand command);
}
