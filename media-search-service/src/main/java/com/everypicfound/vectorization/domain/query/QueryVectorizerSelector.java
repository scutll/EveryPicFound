package com.everypicfound.vectorization.domain.query;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;

@Component
public class QueryVectorizerSelector {
    
    private final Map<SearchType, QueryVectorizer> vectorizerMap;

    //spring启动的时候发现需要List<QueryVectorizer>会自动进行组装注入
    public QueryVectorizerSelector(List<QueryVectorizer> vectorizers) {
        this.vectorizerMap = vectorizers.stream()
                .collect(Collectors.toMap(QueryVectorizer::supportType, Function.identity()));
    }

    public QueryVectorizer select(SearchType searchType) {
        if (searchType == null) {
            throw new BizException(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        QueryVectorizer vectorizer = vectorizerMap.get(searchType);
        if (vectorizer == null) {
            throw new BizException(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        return vectorizer;
    }
    
}
