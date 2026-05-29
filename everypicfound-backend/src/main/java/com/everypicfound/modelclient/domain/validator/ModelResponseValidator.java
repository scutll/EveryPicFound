package com.everypicfound.modelclient.domain.validator;

import com.everypicfound.modelclient.infrastructure.http.PythonHealthHttpResponse;
import com.everypicfound.modelclient.infrastructure.http.PythonVectorizeHttpResponse;

public interface ModelResponseValidator {
    
    void validateVectorizeResponse(PythonVectorizeHttpResponse response);
    
    void validateHealthResponse(PythonHealthHttpResponse response);
}
