package com.everypicfound.vectorization.domain.cache;

import com.everypicfound.vectorization.domain.query.QueryEmbedding;

public interface QueryVectorCacheService {

        /**
         * 根据完整缓存 Key 读取查询向量缓存。
         *
         * @param key        调用方生成的完整缓存 Key
         * @param vectorDim  期望向量维度
         * @return 命中且缓存值合法时返回查询向量，否则返回 null
         */
        QueryEmbedding get(String key,
                        Integer vectorDim);

        /**
         * 写入查询向量缓存。
         *
         * @param key        调用方生成的完整缓存 Key
         * @param vectorDim  期望向量维度
         * @param embedding  查询向量
         */
        void put(String key,
                        Integer vectorDim,
                        QueryEmbedding embedding);

        /**
         * 删除查询向量缓存。
         *
         * @param key        调用方生成的完整缓存 Key
         */
        void evict(String key);
}