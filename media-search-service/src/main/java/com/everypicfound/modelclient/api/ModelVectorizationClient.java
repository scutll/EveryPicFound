package com.everypicfound.modelclient.api;


import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.ModelHealthResult;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;
import com.everypicfound.modelclient.domain.VectorizeResult;

public interface ModelVectorizationClient {
    VectorizeResult vectorizeImage(ImageVectorizeRequest request);

    VectorizeResult vectorizeText(TextVectorizeRequest request);

    ModelHealthResult checkHealth();
}
