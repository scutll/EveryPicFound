package com.everypicfound.search.domain.cache;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.application.context.SearchResponse;
import com.everypicfound.search.domain.collection.SearchCollectionContext;

// 搜索结果缓存服务接口，定义了搜索结果的读取、写入和删除方法。
public interface SearchResultCacheService {

/**
     * 根据搜索请求读取搜索结果缓存。
     *
     * @param command 搜索请求命令
     * @param collectionContext 搜索使用的向量集合上下文
     * @param topK 最终返回结果数量
     * @return 命中则返回缓存的搜索响应，未命中返回 null
     */
        SearchResponse get(SearchCommand command,
                SearchCollectionContext collectionContext,
                Integer topK);

/**
     * 写入搜索结果缓存。
     *
     * @param command 搜索请求命令
     * @param collectionContext 搜索使用的向量集合上下文
     * @param topK 最终返回结果数量
     * @param response 搜索响应结果
     */
        void put(SearchCommand command,
                SearchCollectionContext collectionContext,
                Integer topK,
                SearchResponse response);

/**
     * 根据文本搜索条件删除搜索结果缓存。
     *
     * @param queryText 搜索文本
     * @param collectionContext 搜索使用的向量集合上下文
     * @param topK 最终返回结果数量
     */
        void evictByText(String queryText,
                SearchCollectionContext collectionContext,
                Integer topK);
}