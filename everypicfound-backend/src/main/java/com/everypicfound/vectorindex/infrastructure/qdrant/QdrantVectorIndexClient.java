package com.everypicfound.vectorindex.infrastructure.qdrant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.log.LogService;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.vectorindex.api.VectorIndexClient;
import com.everypicfound.vectorindex.domain.VectorDeleteRequest;
import com.everypicfound.vectorindex.domain.VectorExistsRequest;
import com.everypicfound.vectorindex.domain.VectorOperationResult;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;
import com.everypicfound.vectorindex.error.VectorIndexErrorCode;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.RetrievedPoint;

import static io.qdrant.client.PointIdFactory.id;

@Service
@RequiredArgsConstructor
public class QdrantVectorIndexClient implements VectorIndexClient {

    private static final Logger FALLBACK_LOGGER = LoggerFactory.getLogger(QdrantVectorIndexClient.class);

    private static final String MODULE = "vector-index";
    private static final String BIZ_TYPE = "VECTOR_INDEX";

    private static final String OPERATION_UPSERT = "upsert";
    private static final String OPERATION_DELETE = "delete";
    private static final String OPERATION_EXISTS = "exists";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";

    private final MetricRecorder metricRecorder;

    private final LogService logService;

    private final QdrantClient qdrantClient;

    private final QdrantVectorMapper qdrantVectorMapper;

    @Override
    public VectorOperationResult upsert(VectorUpsertRequest request) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            ErrorCode errorCode = validateUpsertRequest(request);
            if (errorCode != null) {
                result = RESULT_REJECTED;
                return fail(request == null ? null : request.getVectorId(), errorCode, elapsedMillis(startNanos));
            }

            PointStruct point = qdrantVectorMapper.toPointStruct(request);
            qdrantClient.upsertAsync(request.getCollectionName(), List.of(point)).get();

            result = RESULT_SUCCESS;
            return success(request.getVectorId(), "vector upsert success", elapsedMillis(startNanos));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new SystemException(VectorIndexErrorCode.VECTOR_UPSERT_FAILED, exception);
        } catch (Exception exception) {
            throw new SystemException(VectorIndexErrorCode.VECTOR_UPSERT_FAILED, exception);
        } finally {
            recordVectorIndexMetrics(
                    OPERATION_UPSERT,
                    result,
                    startNanos);
        }
    }

    @Override
    public VectorOperationResult delete(VectorDeleteRequest request) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {

            ErrorCode errorCode = validateVectorIdRequest(
                    request == null ? null : request.getCollectionName(),
                    request == null ? null : request.getVectorId());

            if (errorCode != null) {
                result = RESULT_REJECTED;
                return fail(request == null ? null : request.getVectorId(), errorCode, elapsedMillis(startNanos));
            }

            qdrantClient.deleteAsync(request.getCollectionName(), List.of(id(request.getVectorId()))).get();

            result = RESULT_SUCCESS;
            return success(request.getVectorId(), "vector delete success", elapsedMillis(startNanos));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new SystemException(VectorIndexErrorCode.VECTOR_DELETE_FAILED, exception);
        } catch (Exception exception) {
            throw new SystemException(VectorIndexErrorCode.VECTOR_DELETE_FAILED, exception);
        } finally {
            recordVectorIndexMetrics(
                    OPERATION_DELETE,
                    result,
                    startNanos);
        }
    }

    @Override
    public VectorOperationResult exists(VectorExistsRequest request) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            ErrorCode errorCode = validateVectorIdRequest(
                    request == null ? null : request.getCollectionName(),
                    request == null ? null : request.getVectorId());

            if (errorCode != null) {
                result = RESULT_REJECTED;
                return fail(request == null ? null : request.getVectorId(), errorCode, elapsedMillis(startNanos));
            }

            List<RetrievedPoint> points = qdrantClient
                    .retrieveAsync(request.getCollectionName(), id(request.getVectorId()), null).get();

            result = RESULT_SUCCESS;
            return VectorOperationResult.builder()
                    .success(true)
                    .vectorId(request.getVectorId())
                    .exists(points != null && !points.isEmpty())
                    .message("vector exists check success")
                    .costMs(elapsedMillis(startNanos))
                    .build();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new SystemException(VectorIndexErrorCode.VECTOR_EXISTS_CHECK_FAILED, exception);
        } catch (Exception exception) {
            throw new SystemException(VectorIndexErrorCode.VECTOR_EXISTS_CHECK_FAILED, exception);
        } finally {
            recordVectorIndexMetrics(
                    OPERATION_EXISTS,
                    result,
                    startNanos);
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

    private VectorOperationResult fail(
            Long vectorId,
            ErrorCode errorCode,
            Long costMs) {

        return VectorOperationResult.builder()
                .success(false)
                .vectorId(vectorId)
                .exists(false)
                .errorCode(errorCode)
                .message(errorCode.getMessage())
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
