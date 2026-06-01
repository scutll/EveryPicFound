package com.everypicfound.vectorization.infrastructure.publisher;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


import org.springframework.stereotype.Service;


import com.everypicfound.common.executor.ExecutorBizType;
import com.everypicfound.common.executor.ExecutorProvider;
import com.everypicfound.vectorization.api.ImageVectorizationTaskCommand;
import com.everypicfound.vectorization.api.VectorizationPublishResult;
import com.everypicfound.vectorization.api.VectorizationTaskPublisher;
import com.everypicfound.vectorization.application.processor.ImageVectorizationProcessor;
import com.everypicfound.vectorization.error.VectorizationErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThreadPoolVectorizationTaskPublisher implements VectorizationTaskPublisher {

    private final ExecutorProvider executorProvider;

    private final ImageVectorizationProcessor imageVectorizationProcessor;
    
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

        try{
            Executor executor = executorProvider.getExecutor(ExecutorBizType.VECTORIZATION);
            //在execute中检测到有decorator就会进行RequestContext透传
            executor.execute(()-> imageVectorizationProcessor.process(command));

            return VectorizationPublishResult.builder()
                    .success(true)
                    .imageId(command.getImageId())
                    .message("vectorization task submitted")
                    .build();
        } catch (RejectedExecutionException e) {
            return publishFailed(command, "vectorization task rejected");
        } catch (Exception e) {
            return publishFailed(command, "vectorization task publish failed");
        }
        

    }
    



    private VectorizationPublishResult publishFailed(ImageVectorizationTaskCommand command, String message){
        return VectorizationPublishResult.builder()
                .success(false)
                .imageId(command.getImageId())
                .errorCode(VectorizationErrorCode.VECTORIZATION_TASK_PUBLISH_FAILED)
                .message(message)
                .build();
    } 

}
