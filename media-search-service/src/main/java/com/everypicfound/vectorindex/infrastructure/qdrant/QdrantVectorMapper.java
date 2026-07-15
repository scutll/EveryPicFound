package com.everypicfound.vectorindex.infrastructure.qdrant;

import org.springframework.stereotype.Component;

import com.everypicfound.vectorindex.collection.DistanceMetric;
import com.everypicfound.vectorindex.domain.VectorPayload;
import com.everypicfound.vectorindex.domain.VectorUpsertRequest;

import java.util.HashMap;
import java.util.Map;

import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Collections.Distance;

@Component
public class QdrantVectorMapper {
    
    public PointStruct toPointStruct(VectorUpsertRequest request) {
        PointStruct.Builder builder = PointStruct.newBuilder()
                .setId(id(request.getVectorId()))
                .setVectors(vectors(request.getEmbedding()));

        Map<String, Value> payloadMap = toPayloadMap(request.getPayload());
        if (!payloadMap.isEmpty()) {
            builder.putAllPayload(payloadMap);
        }

        return builder.build();
    }
    
    public Distance toQdrantDistance(DistanceMetric distanceMetric) {
        return switch (distanceMetric) {
            case COSINE -> Distance.Cosine;
            case L2 -> Distance.Euclid;
            case INNER_PRODUCT -> Distance.Dot;
        };
    }
    
    private Map<String, Value> toPayloadMap(VectorPayload payload) {
        Map<String, Value> payloadMap = new HashMap<>();

        if (payload == null || payload.getCreatedTime() == null) {
            return payloadMap;
        }

        payloadMap.put("created_time", value(payload.getCreatedTime().toString()));
        return payloadMap;

    }

}
