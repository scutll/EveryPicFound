package com.everypicfound.search.application.service;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.ErrorCode;
import com.everypicfound.common.metric.MetricName;
import com.everypicfound.common.metric.MetricRecorder;
import com.everypicfound.common.metric.MetricTag;
import com.everypicfound.common.metric.MetricTags;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.application.context.SearchResultItem;
import com.everypicfound.search.application.pipeline.SearchPipeline;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.observability.SearchObservationContext;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DefaultSearchApplicationService implements SearchApplicationService {

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_REJECTED = "rejected";
    private static final String RESULT_FAILED = "failed";

    private static final String SEARCH_TYPE_UNKNOWN = "unknown";

    private final SearchPipeline searchPipeline;

    private final MetricRecorder metricRecorder;

    @Override
    //捕获的exception继续向上传递
    public SearchResponse search(SearchCommand command) {
        long startNanos = System.nanoTime();
        String result = RESULT_FAILED;
        String searchType = resolveSearchType(command);

        SearchObservationContext.begin();

        try {
            recordSearchInputMetrics(command, searchType);

            SearchResponse response = searchPipeline.execute(command);

            result = RESULT_SUCCESS;

            recordSearchResultMetrics(response, searchType);
            return response;


        } catch (BizException exception) {
            result = RESULT_REJECTED;

            recordSearchRejection(searchType, exception);
            throw exception;
        } catch (RuntimeException exception) {
            /*
             * 不记录错误日志。
             * SystemException 和未知异常继续交给
             * GlobalExceptionHandler 统一记录。
             */
            result = RESULT_FAILED;
            throw exception;
        } finally {
            
            String cacheResult = SearchObservationContext.getCacheResult();

            recordSearchRequestMetrics(searchType, result, cacheResult, startNanos);

            SearchObservationContext.clear();
        }
    }

    private void recordSearchRequestMetrics(
            String searchType,
            String result,
            String cacheResult,
            long startNanos) {

        MetricTags tags = MetricTags.builder()
                .tag(MetricTag.SEARCH_TYPE, searchType)
                .tag(MetricTag.RESULT, result)
                .tag(MetricTag.CACHE_RESULT, cacheResult)
                .build();

        metricRecorder.increment(
                MetricName.SEARCH_REQUESTS,
                tags);

        metricRecorder.recordTimer(
                MetricName.SEARCH_DURATION,
                elapsedMillis(startNanos),
                tags);
    }

    private void recordSearchInputMetrics(
            SearchCommand command,
            String searchType) {

        if (command == null || command.getSearchType() == null) {
            return;
        }

        SearchType type = command.getSearchType();

        if ((type == SearchType.IMAGE
                || type == SearchType.HYBRID)
                && command.getQueryImageFileSize() != null
                && command.getQueryImageFileSize() >= 0L) {

            metricRecorder.recordValue(
                    MetricName.SEARCH_QUERY_IMAGE_SIZE,
                    command.getQueryImageFileSize(),
                    searchTypeTags(searchType));
        }

        if ((type == SearchType.TEXT
                || type == SearchType.HYBRID)
                && command.getQueryText() != null) {

            metricRecorder.recordValue(
                    MetricName.SEARCH_QUERY_TEXT_LENGTH,
                    command.getQueryText().length(),
                    searchTypeTags(searchType));
        }
    }

    private void recordSearchResultMetrics(
            SearchResponse response,
            String searchType) {

        int resultCount = resolveResultCount(response);

        metricRecorder.recordValue(
                MetricName.SEARCH_RESULT_COUNT,
                resultCount,
                searchTypeTags(searchType));

        if (resultCount == 0) {
            metricRecorder.increment(
                    MetricName.SEARCH_EMPTY_RESULTS,
                    searchTypeTags(searchType));
        }
    }

    private void recordSearchRejection(
            String searchType,
            BizException exception) {

        metricRecorder.increment(
                MetricName.SEARCH_REJECTIONS,
                MetricTags.builder()
                        .tag(
                                MetricTag.SEARCH_TYPE,
                                searchType)
                        .tag(
                                MetricTag.REASON,
                                resolveErrorReason(exception))
                        .build());
    }

    // 获取response中搜索结果数量
    private int resolveResultCount(
            SearchResponse response) {

        if (response == null) {
            return 0;
        }

        List<SearchResultItem> items = response.getItems();

        if (items != null) {
            return items.size();
        }

        return response.getTotal() == null
                ? 0
                : Math.max(response.getTotal(), 0);
    }

    
    // 获取error信息
    private String resolveErrorReason(
            BizException exception) {

        if (exception == null
                || exception.getErrorCode() == null) {
            return "unknown";
        }

        ErrorCode errorCode = exception.getErrorCode();

        if (errorCode instanceof Enum<?> enumErrorCode) {
            return enumErrorCode.name()
                    .toLowerCase(Locale.ROOT);
        }

        return String.valueOf(errorCode.getCode());
    }

    // 转化searchType用于指标标签
    private String resolveSearchType(
            SearchCommand command) {

        if (command == null
                || command.getSearchType() == null) {
            return SEARCH_TYPE_UNKNOWN;
        }

        return command.getSearchType()
                .name()
                .toLowerCase(Locale.ROOT);
    }
    

    private MetricTags searchTypeTags(
            String searchType) {

        return MetricTags.builder()
                .tag(
                        MetricTag.SEARCH_TYPE,
                        searchType)
                .build();
    }



    private long elapsedMillis(long startNanos) {
        return TimeUnit.NANOSECONDS.toMillis(
                System.nanoTime() - startNanos);
    }
    
}
