package com.everypicfound.vectorization.infrastructure.publisher;

import org.springframework.stereotype.Service;

import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

@Service
public class ThreadPoolVectorizationTaskPublisher implements VectorizationTaskPublisher {
    
    @Override
    public VectorizationPublishResult publish(ImageVectorizationTaskCommand command) {
        if (command == null || command.getImageId() == null) {
            return VectorizationPublishResult.builder()
                    .success(false)
                    .imageId(null)
                    .errorCode(VectorizationErrorCode.VECTORIZATION_TASK_INVALID)
                    .message(VectorizationErrorCode.VECTORIZATION_TASK_INVALID.getMessage())
                    .build();
        }

        return VectorizationPublishResult.builder()
                .success(true)
                .imageId(command.getImageId())
                .errorCode(null)
                .message("vectorization task accept, actual execution not enabled yet")
                .build();


    }
}
