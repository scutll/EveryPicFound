package com.everypicfound.vectorindex.api;

import com.everypicfound.vectorindex.domain.VectorDeleteRequest;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;

public interface VectorIndexClient {
    
    VectorOperationResult upsert(VectorUpsertRequest request);

    VectorOperationResult delete(VectorDeleteRequest request);

    VectorOperationResult exists(VectorExistsRequest request);
}
