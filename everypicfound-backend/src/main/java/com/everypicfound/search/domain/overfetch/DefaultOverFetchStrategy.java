package com.everypicfound.search.domain.overfetch;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.error.SearchErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultOverFetchStrategy implements OverFetchStrategy {
    
    private static final int MIN_TOP_K = 10;
    private static final int MIN_OVER_FETCH_RATIO = 2;

    private final SearchProperties searchProperties;

    @Override
    public Integer calculateTopN(OverFetchContext context) {
        Integer topK = resolveTopK(context);
        Integer overFetchRatio = resolveOverFetchRatio(context);
        Integer maxTopN = resolveMaxTopN(context);

        if (topK < MIN_TOP_K || maxTopN < MIN_TOP_K) {
            throw new BizException(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        long calculatedTopN = (long) topK * overFetchRatio;
        
        return (int) Math.min(calculatedTopN, maxTopN);
    }
    

    private Integer resolveTopK(OverFetchContext context) {
        Integer topK = context == null ? null : context.getTopK();
        if (topK == null) {
            topK = searchProperties.getDefaultTopK();
        }

        if (topK == null || topK < MIN_TOP_K || topK > searchProperties.getMaxTopK()) {
            throw new BizException(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        return topK;
    }

    private Integer resolveOverFetchRatio(OverFetchContext context) {
        Integer overFetchRatio = context == null ? null : context.getOverFetchRatio();
        if (overFetchRatio == null) {
            overFetchRatio = searchProperties.getOverFetchRatio();
        }

        if (overFetchRatio == null || overFetchRatio < MIN_OVER_FETCH_RATIO) {
            return MIN_OVER_FETCH_RATIO;
        }

        return overFetchRatio;
    }

    private Integer resolveMaxTopN(OverFetchContext context) {
        Integer maxTopN = context == null ? null : context.getMaxTopN();
        if (maxTopN == null) {
            maxTopN = searchProperties.getMaxTopN();
        }

        if (maxTopN == null || maxTopN < MIN_TOP_K) {
            throw new BizException(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        return maxTopN;
    }
}
