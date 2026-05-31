package com.everypicfound.vectorindex.infrastructure.qdrant;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.domain.VectorDeleteRequest;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;

import lombok.RequiredArgsConstructor;

import java.util.List;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.RetrievedPoint;

import static io.qdrant.client.PointIdFactory.id;


@Service
@RequiredArgsConstructor
public class QdrantVectorIndexClient implements VectorIndexClient {
    
    
    private final QdrantClient qdrantClient;

    private final QdrantVectorMapper qdrantVectorMapper;


    @Override
    public VectorOperationResult upsert(VectorUpsertRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            ErrorCode errorCode = validateUpsertRequest(request);
            if (errorCode != null) {
                return fail(request == null ? null : request.getVectorId(), errorCode);
            }
            PointStruct point = qdrantVectorMapper.toPointStruct(request);
            qdrantClient.upsertAsync(request.getCollectionName(), List.of(point)).get();

            return success(request.getVectorId(), "vector upsert success", costMs(startTime));
        } catch (Exception e) {
            return fail(
                    request == null ? null : request.getVectorId(),
                    VectorIndexErrorCode.VECTOR_UPSERT_FAILED,
                    e.getMessage());
        }
    }
    

    @Override
    public VectorOperationResult delete(VectorDeleteRequest request) {
        long startTime = System.currentTimeMillis();

        try {

            ErrorCode errorCode = validateVectorIdRequest(
                    request == null ? null : request.getCollectionName(),
                    request == null ? null : request.getVectorId());

            if (errorCode != null) {
                return fail(request == null ? null : request.getVectorId(), errorCode);
            }

            qdrantClient.deleteAsync(request.getCollectionName(), List.of(id(request.getVectorId()))).get();

            return success(request.getVectorId(), "vector delete success", costMs(startTime));
        } catch (Exception e) {
            return fail(
                    request == null ? null : request.getVectorId(),
                    VectorIndexErrorCode.VECTOR_DELETE_FAILED,
                    e.getMessage());
        }
    }
    
    @Override
    public VectorOperationResult exists(VectorExistsRequest request) {
        long startTime = System.currentTimeMillis();

        try{
            ErrorCode errorCode = validateVectorIdRequest(
                    request == null ? null : request.getCollectionName(),
                    request == null ? null : request.getVectorId()
            );

            if (errorCode != null) {
                return fail(request == null ? null : request.getVectorId(), errorCode);
            }

            List<RetrievedPoint> points = qdrantClient
                    .retrieveAsync(request.getCollectionName(), id(request.getVectorId()), null).get();
            return VectorOperationResult.builder()
                    .success(true)
                    .vectorId(request.getVectorId())
                    .exists(points != null && !points.isEmpty())
                    .message("vector exists check success")
                    .costMs(costMs(startTime))
                    .build();
        } catch (Exception e) {
            return fail(
                    request == null ? null : request.getVectorId(),
                    VectorIndexErrorCode.VECTOR_EXISTS_CHECK_FAILED,
                    e.getMessage());
        }
    }


    private ErrorCode validateUpsertRequest(VectorUpsertRequest request) {
        if (request == null) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        ErrorCode baseErrorCode = validateVectorIdRequest(request.getCollectionName(), request.getVectorId());
        if (baseErrorCode != null) {
            return baseErrorCode;
        }

        if (request.getEmbedding() == null || request.getEmbedding().isEmpty()) {
            return VectorIndexErrorCode.VECTOR_DIM_MISMATCH;
        }

        return null;
    }


    private ErrorCode validateVectorIdRequest(String collectionName, Long vectorId) {
        if (collectionName == null || collectionName.isBlank()) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        if (vectorId == null || vectorId <= 0) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        return null;
    }

    private VectorOperationResult success(Long vectorId, String message, Long costMs) {
        return VectorOperationResult.builder()
                .success(true)
                .vectorId(vectorId)
                .exists(true)
                .message(message)
                .costMs(costMs)
                .build();
    }

    private VectorOperationResult fail(Long vectorId, ErrorCode errorCode) {
        return fail(vectorId, errorCode, errorCode.getMessage());
    }

    private VectorOperationResult fail(Long vectorId, ErrorCode errorCode, String message) {
        return VectorOperationResult.builder()
                .success(false)
                .vectorId(vectorId)
                .exists(false)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    private Long costMs(long startTime) {
        return System.currentTimeMillis() - startTime;
    }


}
