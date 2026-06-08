package com.everypicfound.vectorindex.infrastructure.qdrant;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.vectorindex.api.VectorSearchClient;
import com.everypicfound.vectorindex.domain.VectorSearchItem;
import com.everypicfound.vectorindex.domain.VectorSearchRequest;
import com.everypicfound.vectorindex.domain.VectorSearchResult;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Common.PointId;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QdrantVectorSearchClient implements VectorSearchClient{

    private final QdrantClient qdrantClient;

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        long startTime = System.currentTimeMillis();

        try{
            ErrorCode errorCode = validateSearchRequest(request);

            if(errorCode != null){
                return fail(
                        request == null ? null : request.getCollectionName(),
                        request == null ? null : request.getTopN(),
                        errorCode,
                        errorCode.getMessage(),
                        costMs(startTime)
                );
            }

            SearchPoints searchPoints = SearchPoints.newBuilder()
                    .setCollectionName(request.getCollectionName())
                    .addAllVector(request.getQueryEmbedding())
                    .setLimit(request.getTopN())
                    .build();

            List<ScoredPoint> scoredPoints = qdrantClient.searchAsync(searchPoints).get();

            List<VectorSearchItem> items = scoredPoints == null ? 
                    Collections.emptyList() 
                    : scoredPoints.stream()
                    .map(this::toVectorSearchItem)
                            .toList();

            return success(request.getCollectionName(), request.getTopN(), items, costMs(startTime));
        } catch (Exception e) {
            return fail(
                    request == null? null : request.getCollectionName(),
                    request == null? null : request.getTopN(),
                    VectorIndexErrorCode.VECTOR_SEARCH_FAILED,                    
                    e.getMessage(),
                    costMs(startTime)
            );
        }

    }
    

    private ErrorCode validateSearchRequest(VectorSearchRequest request) {
        if (request == null) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        if (request.getCollectionName() == null || request.getCollectionName().isBlank()) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        if (request.getQueryEmbedding() == null || request.getQueryEmbedding().isEmpty()) {
            return VectorIndexErrorCode.VECTOR_DIM_MISMATCH;
        }

        if (request.getTopN() == null || request.getTopN() <= 0) {
            return VectorIndexErrorCode.VECTOR_COLLECTION_CONFIG_INVALID;
        }

        return null;
    }

    private VectorSearchItem toVectorSearchItem(ScoredPoint scoredPoint) {
        return VectorSearchItem.builder()
                .vectorId(toLongVectorId(scoredPoint.getId()))
                .score(scoredPoint.getScore())
                .build();
    }


    private Long toLongVectorId(PointId pointId) {
        if (pointId == null) {
            return null;
        }

        return pointId.getNum();
    }
    
    private VectorSearchResult success(String collectionName,
                                       Integer topN,
                                       List<VectorSearchItem> items,
                                       Long costMs) {
        return VectorSearchResult.builder()
                .success(true)
                .collectionName(collectionName)
                .topN(topN)
                .items(items)
                .costMs(costMs)
                .message("vector search success")
                .build();
    }

    private VectorSearchResult fail(String collectionName,
                                    Integer topN,
                                    ErrorCode errorCode,
                                    String message,
                                    Long costMs) {
        return VectorSearchResult.builder()
                .success(false)
                .collectionName(collectionName)
                .topN(topN)
                .items(Collections.emptyList())
                .errorCode(errorCode)
                .message(message)
                .costMs(costMs)
                .build();
    }

    private Long costMs(long startTime) {
        return System.currentTimeMillis() - startTime;
    } 
}
