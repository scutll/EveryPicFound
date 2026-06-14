package com.everypicfound.vectorization.domain.cache;

import com.everypicfound.vectorization.domain.query.QueryEmbedding;

public interface TextVectorCacheService {

    /**
     * 根据模型、向量维度和查询文本读取文本向量缓存。
     *
     * @param modelName 模型名称
     * @param vectorDim 向量维度
     * @param queryText 查询文本
     * @return 命中则返回查询向量，未命中返回 null
     */
    QueryEmbedding get(String modelName,
                    Integer vectorDim,
                    String queryText);

    /**
     * 写入文本向量缓存。
     *
     * @param modelName 模型名称
     * @param vectorDim 向量维度
     * @param queryText 查询文本
     * @param embedding 查询向量
     */
    void put(String modelName,
            Integer vectorDim,
            String queryText,
            QueryEmbedding embedding);

    /**
     * 删除文本向量缓存。
     *
     * @param modelName 模型名称
     * @param vectorDim 向量维度
     * @param queryText 查询文本
     */
    void evict(String modelName,
            Integer vectorDim,
            String queryText);
}