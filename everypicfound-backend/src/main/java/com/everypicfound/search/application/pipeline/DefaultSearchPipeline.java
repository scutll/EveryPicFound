package com.everypicfound.search.application.pipeline;

import org.springframework.stereotype.Service;

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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchPipeline implements SearchPipeline {
    private final SearchValidatorManager searchValidatorManager;

    private final SearchCollectionResolver searchCollectionResolver;

    private final QueryVectorizerSelector queryVectorizerSelector;

    private final OverFetchStrategy overFetchStrategy;

    private final VectorSearchClient vectorSearchClient;

    private final ImageAssetQueryService imageAssetQueryService;

    private final SearchResultFilter searchResultFilter;

    private final RerankStrategy rerankStrategy;

    private final SearchResultAssembler searchResultAssembler;

    private final SearchProperties searchProperties;

    @Override
    public SearchResponse execute(SearchCommand command) {
        long startTime = System.currentTimeMillis();

        validateCommand(command);

        SearchCollectionContext collectionContext = searchCollectionResolver.resolve();

        QueryEmbedding queryEmbedding = vectorizeQuery(command);

        validateQueryEmbedding(queryEmbedding, collectionContext);

        Integer topK = resolveTopK(command);

        Integer topN = calculateTopN(command, topK);

        VectorSearchResult vectorSearchResult = searchVector(collectionContext, queryEmbedding, topN);

        ImageAssetBatchQueryResult imageAssetBatchQueryResult = batchQueryImageAssets(vectorSearchResult);

        SearchFilterResult searchFilterResult = filterResults(command, vectorSearchResult,
                imageAssetBatchQueryResult);

        RerankResult rerankResult = rerank(command, queryEmbedding, searchFilterResult);

        List<SearchResultItem> finalItems = truncateTopK(extractRerankItems(rerankResult), topK);

        return assembleResponse(command, finalItems, topK, startTime, vectorSearchResult, searchFilterResult);
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
            throw new BizException(SearchErrorCode.QUERY_VECTORIZATION_FAILED);
        }

        return queryEmbedding;
    }

    private void validateQueryEmbedding(QueryEmbedding queryEmbedding, SearchCollectionContext collectionContext) {
        if (queryEmbedding == null
                || queryEmbedding.getEmbedding() == null
                || queryEmbedding.getEmbedding().isEmpty()
                || queryEmbedding.getDim() == null) {
            throw new BizException(SearchErrorCode.QUERY_EMBEDDING_EMPTY);
        }

        if (collectionContext == null
                || collectionContext.getVectorDim() == null
                || !collectionContext.getVectorDim().equals(queryEmbedding.getDim())) {
            throw new BizException(SearchErrorCode.QUERY_VECTOR_DIM_MISMATCH);
        }
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

        if (result == null || !Boolean.TRUE.equals(result.getSuccess())) {
            throw new BizException(SearchErrorCode.VECTOR_SEARCH_FAILED);
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

        return imageAssetQueryService.batchQueryByIds(
                BatchImageAssetQuery.builder()
                        .imageIds(imageIds)
                        .build());
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
}
