package com.everypicfound.vectorindex.infrastructure.qdrant;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
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
public class QdrantVectorSearchClient implements VectorSearchClient {
    

    private static final String OPERATION_SEARCH = "search";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";

    private final MetricRecorder metricRecorder;


    private final QdrantClient qdrantClient;

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try{
            ErrorCode errorCode = validateSearchRequest(request);

            if (errorCode != null) {
                result = RESULT_REJECTED;

                return fail(
                        request == null ? null : request.getCollectionName(),
                        request == null ? null : request.getTopN(),
                        errorCode,
                        errorCode.getMessage(),
                        elapsedMillis(startNanos)
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

            result = RESULT_SUCCESS;

            metricRecorder.recordValue(MetricName.VECTOR_INDEX_SEARCH_RESULT_COUNT, items.size(), MetricTags.empty());
            return success(request.getCollectionName(), request.getTopN(), items, elapsedMillis(startNanos));

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            throw new SystemException(VectorIndexErrorCode.VECTOR_SEARCH_FAILED, exception);
        } catch (Exception exception) {
            throw new SystemException(VectorIndexErrorCode.VECTOR_SEARCH_FAILED, exception);
        } finally {
            recordVectorIndexMetrics(OPERATION_SEARCH, result, startNanos);
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

    
    
    private void recordVectorIndexMetrics(
            String operation,
            String result,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.OPERATION, operation)
                .tag(MetricTag.RESULT, result)
                .build();

        metricRecorder.increment(
                MetricName.VECTOR_INDEX_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.VECTOR_INDEX_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }
    
}
