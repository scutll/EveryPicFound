package com.everypicfound.search.application.pipeline;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.exception.SystemException;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.observability.SearchObservationContext;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.imageasset.application.command.BatchImageAssetQuery;
import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import com.everypicfound.imageasset.application.result.ImageAssetBatchQueryResult;
import com.everypicfound.imageasset.domain.service.ImageAssetQueryService;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.assembler.SearchAssemblerContext;
import com.everypicfound.search.domain.assembler.SearchResultAssembler;
import com.everypicfound.search.domain.cache.SearchResultCacheService;
import com.everypicfound.search.domain.collection.SearchCollectionContext;
import com.everypicfound.search.domain.collection.SearchCollectionResolver;
import com.everypicfound.search.domain.filter.SearchFilterContext;
import com.everypicfound.search.domain.filter.SearchFilterResult;
import com.everypicfound.search.domain.filter.SearchResultFilter;
import com.everypicfound.search.domain.overfetch.OverFetchContext;
import com.everypicfound.search.domain.overfetch.OverFetchStrategy;
import com.everypicfound.search.domain.rerank.RerankContext;
import com.everypicfound.search.domain.rerank.RerankResult;
import com.everypicfound.search.domain.rerank.RerankStrategy;
import com.everypicfound.search.domain.validator.SearchValidateResult;
import com.everypicfound.search.domain.validator.SearchValidatorManager;
import com.everypicfound.search.error.SearchErrorCode;
import com.everypicfound.vectorindex.api.VectorSearchClient;
import com.everypicfound.vectorindex.domain.VectorSearchItem;
import com.everypicfound.vectorindex.domain.VectorSearchRequest;
import com.everypicfound.vectorindex.domain.VectorSearchResult;
import com.everypicfound.vectorization.domain.query.QueryEmbedding;
import com.everypicfound.vectorization.domain.query.QueryVectorizeRequest;
import com.everypicfound.vectorization.domain.query.QueryVectorizer;
import com.everypicfound.vectorization.domain.query.QueryVectorizerSelector;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchPipeline implements SearchPipeline {

    // 指标字段
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";

    private static final String STAGE_VALIDATE = "validate";
    private static final String STAGE_RESOLVE_COLLECTION = "resolve_collection";
    private static final String STAGE_RESULT_CACHE_GET = "result_cache_get";
    private static final String STAGE_QUERY_VECTORIZE = "query_vectorize";
    private static final String STAGE_EMBEDDING_VALIDATE = "embedding_validate";
    private static final String STAGE_OVERFETCH = "overfetch";
    private static final String STAGE_VECTOR_RECALL = "vector_recall";
    private static final String STAGE_BACKFILL = "backfill";
    private static final String STAGE_FILTER = "filter";
    private static final String STAGE_RERANK = "rerank";
    private static final String STAGE_ASSEMBLE = "assemble";
    private static final String STAGE_RESULT_CACHE_PUT = "result_cache_put";

    private static final String ITEM_STAGE_RECALL = "recall";
    private static final String ITEM_STAGE_BACKFILL_REQUESTED = "backfill_requested";
    private static final String ITEM_STAGE_BACKFILL_RETURNED = "backfill_returned";
    private static final String ITEM_STAGE_FILTER_OUTPUT = "filter_output";

    private static final String FILTER_REASON_ORPHAN_VECTOR = "orphan_vector";
    private static final String FILTER_REASON_INVALID_IMAGE = "invalid_image";

    private static final String EMBEDDING_REASON_EMPTY = "empty";
    private static final String EMBEDDING_REASON_DIMENSION_MISMATCH = "dimension_mismatch";

    // Services

    private final MetricRecorder metricRecorder;

    private final SearchValidatorManager searchValidatorManager;

    private final SearchCollectionResolver searchCollectionResolver;

    private final QueryVectorizerSelector queryVectorizerSelector;

    private final OverFetchStrategy overFetchStrategy;

    private final VectorSearchClient vectorSearchClient;

    private final ImageAssetQueryService imageAssetQueryService;

    private final SearchResultFilter searchResultFilter;

    private final RerankStrategy rerankStrategy;

    private final SearchResultAssembler searchResultAssembler;

    private final SearchResultCacheService searchResultCacheService;

    private final SearchProperties searchProperties;

    /*
     * 流程改为：
     * validateCommand
     * resolveCollection
     * resolveTopK
     * 查搜索结果缓存
     * 缓存命中：直接返回
     * 缓存未命中：继续向量化和搜索
     * 搜索完成：写入缓存
     * 返回结果
     */
    @Override
    public SearchResponse execute(SearchCommand command) {

        long searchStartTime = System.currentTimeMillis();

        SearchType searchType = command == null ? null : command.getSearchType();

        /*
         * validate 阶段同时完成业务参数校验和 topK 解析。
         */

        Integer topK = observeStage(searchType, STAGE_VALIDATE, () -> {
            validateCommand(command);
            return resolveTopK(command);
        });

        SearchCollectionContext collectionContext = observeStage(searchType, STAGE_RESOLVE_COLLECTION,
                searchCollectionResolver::resolve);

        SearchResponse cachedResponse = observeCacheGet(searchType, command, collectionContext, topK);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        QueryEmbedding queryEmbedding = observeStage(
                searchType,
                STAGE_QUERY_VECTORIZE,
                () -> vectorizeQuery(command));

        observeStage(
                searchType,
                STAGE_EMBEDDING_VALIDATE,
                () -> {
                    validateQueryEmbedding(
                            queryEmbedding,
                            collectionContext,
                            searchType);

                    return null;
                });

        Integer topN = observeStage(
                searchType,
                STAGE_OVERFETCH,
                () -> calculateTopN(
                        command,
                        topK));

        recordSearchTopN(
                searchType,
                topN);

        VectorSearchResult vectorSearchResult = observeStage(
                searchType,
                STAGE_VECTOR_RECALL,
                () -> searchVector(
                        collectionContext,
                        queryEmbedding,
                        topN));

        recordPipelineItemCount(
                searchType,
                ITEM_STAGE_RECALL,
                getVectorRecallCount(vectorSearchResult));

        recordPipelineItemCount(
                searchType,
                ITEM_STAGE_BACKFILL_REQUESTED,
                countBackfillRequested(vectorSearchResult));

        ImageAssetBatchQueryResult imageAssetBatchQueryResult = observeStage(
                searchType,
                STAGE_BACKFILL,
                () -> batchQueryImageAssets(
                        vectorSearchResult));

        recordPipelineItemCount(
                searchType,
                ITEM_STAGE_BACKFILL_RETURNED,
                countBackfillReturned(
                        imageAssetBatchQueryResult));

        SearchFilterResult searchFilterResult = observeStage(
                searchType,
                STAGE_FILTER,
                () -> filterResults(
                        command,
                        vectorSearchResult,
                        imageAssetBatchQueryResult));

        recordPipelineItemCount(
                searchType,
                ITEM_STAGE_FILTER_OUTPUT,
                countFilterOutput(
                        searchFilterResult));

        recordFilteredItemCount(
                searchType,
                FILTER_REASON_ORPHAN_VECTOR,
                getOrphanVectorCount(
                        searchFilterResult));

        recordFilteredItemCount(
                searchType,
                FILTER_REASON_INVALID_IMAGE,
                getInvalidImageCount(
                        searchFilterResult));

        RerankResult rerankResult = observeStage(
                searchType,
                STAGE_RERANK,
                () -> rerank(
                        command,
                        queryEmbedding,
                        searchFilterResult));

        List<SearchResultItem> finalItems = truncateTopK(extractRerankItems(rerankResult), topK);

        SearchResponse response = observeStage(
                searchType,
                STAGE_ASSEMBLE,
                () -> assembleResponse(
                        command,
                        finalItems,
                        topK,
                        searchStartTime,
                        vectorSearchResult,
                        searchFilterResult));

        observeCachePut(
                searchType,
                command,
                collectionContext,
                topK,
                response);

        return response;
    }

    private void validateCommand(SearchCommand command) {
        SearchValidateResult result = searchValidatorManager.validate(command);

        if (result == null || !result.isValid()) {
            throw new BizException(result == null || result.getErrorCode() == null
                    ? SearchErrorCode.SEARCH_PARAM_INVALID
                    : result.getErrorCode());
        }
    }

    private QueryEmbedding vectorizeQuery(SearchCommand command) {
        QueryVectorizer queryVectorizer = queryVectorizerSelector.select(command.getSearchType());

        QueryVectorizeRequest request = QueryVectorizeRequest.builder()
                .searchType(command.getSearchType())
                .queryImage(command.getQueryImage())
                .queryImageOriginalFileName(command.getQueryImageOriginalFileName())
                .queryImageFileSize(command.getQueryImageFileSize())
                .queryImageMimeType(command.getQueryImageMimeType())
                .queryText(command.getQueryText())
                .traceId(command.getTraceId())
                .requestId(command.getRequestId())
                .build();

        QueryEmbedding queryEmbedding = queryVectorizer.vectorize(request);
        if (queryEmbedding == null) {
            throw new SystemException(SearchErrorCode.QUERY_VECTORIZATION_FAILED);
        }

        return queryEmbedding;
    }

    private void validateQueryEmbedding(QueryEmbedding queryEmbedding, SearchCollectionContext collectionContext,
            SearchType searchType) {
        if (queryEmbedding == null
                || queryEmbedding.getEmbedding() == null
                || queryEmbedding.getEmbedding().isEmpty()
                || queryEmbedding.getDim() == null) {

            recordEmbeddingInvalid(searchType, EMBEDDING_REASON_EMPTY);
            throw new SystemException(SearchErrorCode.QUERY_EMBEDDING_EMPTY);
        }

        if (collectionContext == null
                || collectionContext.getVectorDim() == null
                || !collectionContext.getVectorDim().equals(queryEmbedding.getDim())) {

            recordEmbeddingInvalid(searchType, EMBEDDING_REASON_DIMENSION_MISMATCH);
            throw new SystemException(SearchErrorCode.QUERY_VECTOR_DIM_MISMATCH);
        }
    }

    private void recordEmbeddingInvalid(
            SearchType searchType,
            String reason) {

        metricRecorder.increment(
                MetricName.SEARCH_EMBEDDING_INVALID,
                MetricTags.builder()
                        .tag(
                                MetricTag.SEARCH_TYPE,
                                resolveSearchType(searchType))
                        .tag(
                                MetricTag.REASON,
                                reason)
                        .build());
    }

    private Integer resolveTopK(SearchCommand command) {
        Integer topK = command.getTopK() == null
                ? searchProperties.getDefaultTopK()
                : command.getTopK();

        if (topK == null || topK <= 0 || topK > searchProperties.getMaxTopK()) {
            throw new BizException(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        return topK;
    }

    private Integer calculateTopN(SearchCommand command, Integer topK) {
        return overFetchStrategy.calculateTopN(
                OverFetchContext.builder()
                        .searchType(command.getSearchType())
                        .topK(topK)
                        .maxTopN(searchProperties.getMaxTopN())
                        .overFetchRatio(searchProperties.getOverFetchRatio())
                        .build());
    }

    private VectorSearchResult searchVector(SearchCollectionContext collectionContext,
            QueryEmbedding queryEmbedding,
            Integer topN) {
        VectorSearchResult result = vectorSearchClient.search(
                VectorSearchRequest.builder()
                        .collectionName(collectionContext.getCollectionName())
                        .queryEmbedding(queryEmbedding.getEmbedding())
                        .topN(topN)
                        .build());

        if (result == null){    
            throw new SystemException(SearchErrorCode.VECTOR_SEARCH_FAILED);
        }

        if(!Boolean.TRUE.equals(result.getSuccess()))
        {
            ErrorCode errorCode = result.getErrorCode();

            throw new SystemException(errorCode == null ? SearchErrorCode.VECTOR_SEARCH_FAILED : errorCode);
        }

        return result;
    }

    private ImageAssetBatchQueryResult batchQueryImageAssets(VectorSearchResult vectorSearchResult) {
        List<Long> imageIds = vectorSearchResult.getItems() == null
                ? Collections.emptyList()
                : vectorSearchResult.getItems().stream()
                        .map(VectorSearchItem::getVectorId)
                        .filter(imageId -> imageId != null)
                        .collect(Collectors.toList());

        ImageAssetBatchQueryResult result = imageAssetQueryService.batchQueryByIds(
                BatchImageAssetQuery.builder()
                        .imageIds(imageIds)
                        .build());

        if (result == null) {
            throw new SystemException(SearchErrorCode.IMAGE_ASSET_QUERY_FAILED);
        }

        return result;
    }

    private SearchFilterResult filterResults(SearchCommand command,
            VectorSearchResult vectorSearchResult,
            ImageAssetBatchQueryResult imageAssetBatchQueryResult) {
        List<VectorSearchItem> vectorItems = vectorSearchResult.getItems() == null
                ? Collections.emptyList()
                : vectorSearchResult.getItems();

        List<ImageAssetDTO> imageAssets = imageAssetBatchQueryResult == null
                || imageAssetBatchQueryResult.getItems() == null
                        ? Collections.emptyList()
                        : imageAssetBatchQueryResult.getItems();

        return searchResultFilter.filter(
                SearchFilterContext.builder()
                        .vectorItems(vectorItems)
                        .imageAssets(imageAssets)
                        .searchType(command.getSearchType())
                        .build());
    }

    private RerankResult rerank(SearchCommand command,
            QueryEmbedding queryEmbedding,
            SearchFilterResult searchFilterResult) {
        List<SearchResultItem> items = searchFilterResult == null
                || searchFilterResult.getItems() == null
                        ? Collections.emptyList()
                        : searchFilterResult.getItems();

        return rerankStrategy.rerank(
                RerankContext.builder()
                        .items(items)
                        .searchType(command.getSearchType())
                        .queryEmbedding(queryEmbedding)
                        .build());
    }

    private List<SearchResultItem> extractRerankItems(RerankResult rerankResult) {
        if (rerankResult == null || rerankResult.getItems() == null) {
            return Collections.emptyList();
        }

        return rerankResult.getItems();
    }

    private List<SearchResultItem> truncateTopK(List<SearchResultItem> items, Integer topK) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }

        if (items.size() <= topK) {
            return items;
        }

        return items.subList(0, topK);
    }

    private SearchResponse assembleResponse(SearchCommand command,
            List<SearchResultItem> finalItems,
            Integer topK,
            Long startTime,
            VectorSearchResult vectorSearchResult,
            SearchFilterResult searchFilterResult) {
        Long costMS = System.currentTimeMillis() - startTime;
        return searchResultAssembler.assemble(
                SearchAssemblerContext.builder()
                        .searchType(command.getSearchType())
                        .items(finalItems)
                        .topK(topK)
                        .costMs(costMS)
                        .totalRecallCount(getVectorRecallCount(vectorSearchResult))
                        .orphanVectorCount(getOrphanVectorCount(searchFilterResult))
                        .invalidImageCount(getInvalidImageCount(searchFilterResult))
                        .build());
    }

    private Integer getVectorRecallCount(VectorSearchResult vectorSearchResult) {
        if (vectorSearchResult == null || vectorSearchResult.getItems() == null) {
            return 0;
        }

        return vectorSearchResult.getItems().size();
    }

    private Integer getOrphanVectorCount(SearchFilterResult searchFilterResult) {
        if (searchFilterResult == null || searchFilterResult.getOrphanVectorCount() == null) {
            return 0;
        }

        return searchFilterResult.getOrphanVectorCount();
    }

    private Integer getInvalidImageCount(SearchFilterResult searchFilterResult) {
        if (searchFilterResult == null || searchFilterResult.getInvalidImageCount() == null) {
            return 0;
        }

        return searchFilterResult.getInvalidImageCount();
    }

    // 数量Value类型指标记录方法
    private void recordSearchTopN(
            SearchType searchType,
            Integer topN) {

        if (topN == null || topN < 0) {
            return;
        }

        metricRecorder.recordValue(
                MetricName.SEARCH_TOP_N,
                topN,
                searchTypeTags(searchType));
    }

    private void recordPipelineItemCount(
            SearchType searchType,
            String stage,
            int count) {

        metricRecorder.recordValue(
                MetricName.SEARCH_PIPELINE_ITEM_COUNT,
                Math.max(count, 0),
                MetricTags.builder()
                        .tag(
                                MetricTag.SEARCH_TYPE,
                                resolveSearchType(searchType))
                        .tag(
                                MetricTag.STAGE,
                                stage)
                        .build());
    }

    private void recordFilteredItemCount(
            SearchType searchType,
            String reason,
            int count) {

        metricRecorder.recordValue(
                MetricName.SEARCH_FILTERED_ITEM_COUNT,
                Math.max(count, 0),
                MetricTags.builder()
                        .tag(
                                MetricTag.SEARCH_TYPE,
                                resolveSearchType(searchType))
                        .tag(
                                MetricTag.REASON,
                                reason)
                        .build());
    }

    private MetricTags searchTypeTags(
            SearchType searchType) {

        return MetricTags.builder()
                .tag(
                        MetricTag.SEARCH_TYPE,
                        resolveSearchType(searchType))
                .build();
    }

    private int countBackfillRequested(
            VectorSearchResult result) {

        if (result == null || result.getItems() == null) {
            return 0;
        }

        return (int) result.getItems()
                .stream()
                .map(VectorSearchItem::getVectorId)
                .filter(imageId -> imageId != null)
                .count();
    }

    private int countBackfillReturned(
            ImageAssetBatchQueryResult result) {

        if (result == null || result.getItems() == null) {
            return 0;
        }

        return result.getItems().size();
    }

    private int countFilterOutput(
            SearchFilterResult result) {

        if (result == null || result.getItems() == null) {
            return 0;
        }

        return result.getItems().size();
    }

    // 方法调用wrapper，pipeline上调用方法并记录指标
    private <T> T observeStage(
            SearchType searchType,
            String stage,
            Supplier<T> action) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            T value = action.get();
            result = RESULT_SUCCESS;
            return value;
        } catch (BizException exception) {
            result = RESULT_REJECTED;
            throw exception;
        } finally {
            recordStageMetrics(searchType, stage, result, startNanos);
        }
    }

    private void recordStageMetrics(
            SearchType searchType,
            String stage,
            String result,
            long startNanos) {

        metricRecorder.recordTimer(
                MetricName.SEARCH_STAGE_DURATION,
                elapsedMillis(startNanos),
                MetricTags.builder()
                        .tag(
                                MetricTag.SEARCH_TYPE,
                                resolveSearchType(searchType))
                        .tag(
                                MetricTag.STAGE,
                                stage)
                        .tag(
                                MetricTag.RESULT,
                                result)
                        .build());
    }

    private String resolveSearchType(
            SearchType searchType) {

        if (searchType == null) {
            return "unknown";
        }

        return searchType.name()
                .toLowerCase(Locale.ROOT);
    }

    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }

    /*
     * SearchResultService(Cache调用的Wrapper)
     * 在get、put等方法上并不抛出异常，而是在异常/无缓存记录的时候统一返回null，并且错误信息在ThreadLocal中记录(
     * SearchObservationContext) 因此需要在结果null时候调取Context查看具体错误信息并抛出/处理
     */

    private SearchResponse observeCacheGet(
            SearchType searchType,
            SearchCommand command,
            SearchCollectionContext collectionContext,
            Integer topK) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            SearchResponse response = searchResultCacheService.get(command, collectionContext, topK);

            result = SearchObservationContext.getCacheGetStageResult();

            return response;
        } catch (BizException exception) {
            result = RESULT_REJECTED;
            throw exception;
        } finally {
            recordStageMetrics(searchType, STAGE_RESULT_CACHE_GET, result, startNanos);
        }
    }

    private void observeCachePut(
            SearchType searchType,
            SearchCommand command,
            SearchCollectionContext collectionContext,
            Integer topK,
            SearchResponse response) {

        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;

        try {
            searchResultCacheService.put(
                    command,
                    collectionContext,
                    topK,
                    response);

            result = SearchObservationContext
                    .getCachePutStageResult();
        } catch (BizException exception) {
            result = RESULT_REJECTED;
            throw exception;
        } finally {
            recordStageMetrics(
                    searchType,
                    STAGE_RESULT_CACHE_PUT,
                    result,
                    startNanos);
        }
    }

}
