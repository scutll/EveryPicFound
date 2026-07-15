package com.everypicfound.modelclient.infrastructure.http;

import com.everypicfound.modelclient.domain.ImageVectorizeRequest;
import com.everypicfound.modelclient.domain.TextVectorizeRequest;

public interface PythonModelHttpClient {
    
    PythonVectorizeHttpResponse postImageMultipart(ImageVectorizeRequest request);

    PythonVectorizeHttpResponse postTextForm(TextVectorizeRequest request);

    PythonHealthHttpResponse getHealth();
}
