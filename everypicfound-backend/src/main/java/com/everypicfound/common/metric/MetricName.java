package com.everypicfound.common.metric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.everypicfound.common.metric.MetricType.TIMER;
import static com.everypicfound.common.metric.MetricType.COUNTER;
import static com.everypicfound.common.metric.MetricType.DISTRIBUTION_SUMMARY;

/**
 * EveryPicFound 标准指标定义。
 */
@Getter
@RequiredArgsConstructor
public enum MetricName {


    /**
     * 搜索请求计数
     */
    SEARCH_TOTAL_COUNT(
            "everypicfound.search.requests",
            COUNTER,
            "Search request count",
            null),
        
    /**
     * 搜索耗时
     */
    SEARCH_COST_MS(
            "everypicfound.search.duration",
            TIMER,
            "Search request duration",
            null),
        
    /**
    * 上传请求计数
    */
    UPLOAD_TOTAL_COUNT(
            "everypicfound.image.upload.requests",
            COUNTER,
            "Image upload request count",
            null),
        
    /**
    * 上传耗时
    */
    UPLOAD_COST_MS(
            "everypicfound.image.upload.duration",
            TIMER,
            "Image upload duration",
            null),
        
    /**
    * 向量化请求计数
    */
    VECTORIZATION_TOTAL_COUNT(
            "everypicfound.vectorization.tasks",
            COUNTER,
            "Image vectorization task count",
            null),
        
    /**
    * 向量化耗时
    */
    VECTORIZATION_COST_MS(
            "everypicfound.vectorization.duration",
            TIMER,
            "Image vectorization duration",
            null),
        
    /**
    * 向量化重试计数
    */
    VECTORIZATION_RETRY_COUNT(
            "everypicfound.vectorization.retries",
            COUNTER,
            "Image vectorization retry count",
            null),
        
    /**
    * 文件保存耗时
    */
    FILE_SAVE_DURATION_MS(
            "everypicfound.storage.duration",
            TIMER,
            "Storage operation duration",
            null),
        
    /**
    * 文件读取耗时
    */
    FILE_READ_DURATION_MS(
            "everypicfound.storage.duration",
            TIMER,
            "Storage operation duration",
            null),
        
    /**
    * 文件删除耗时
    */
    FILE_DELETE_DURATION_MS(
            "everypicfound.storage.duration",
            TIMER,
            "Storage operation duration",
            null),
        
    /**
    * 文件保存失败计数
    */
    FILE_SAVE_FAILED_COUNT(
            "everypicfound.storage.failures",
            COUNTER,
            "Storage operation failure count",
            null),
        
    /**
     * 文件读取失败计数
     */
    FILE_READ_FAILED_COUNT(
            "everypicfound.storage.failures",
            COUNTER,
            "Storage operation failure count",
            null),
        
    /**
    * 文件删除失败计数
    */
    FILE_DELETE_FAILED_COUNT(
            "everypicfound.storage.failures",
            COUNTER,
            "Storage operation failure count",
            null),
        
            
    /**
    * 文件不存在计数
    */
    FILE_MISSING_COUNT(
            "everypicfound.storage.missing",
            COUNTER,
            "Missing storage file count",
            null),
        
    /**
    * 文件保存量计数
    */
    STORED_FILE_SIZE_BYTES(
            "everypicfound.storage.file.size",
            DISTRIBUTION_SUMMARY,
            "Stored file size distribution",
            "bytes"),
        
    /**
    * ModelClient调用计数
    */
    MODEL_CLIENT_TOTAL_COUNT(
            "everypicfound.model.client.requests",
            COUNTER,
            "Model service request count",
            null),
            
    /**
    * ModelClient调用耗时
    */
    MODEL_CLIENT_COST_MS(
            "everypicfound.model.client.duration",
            TIMER,
            "Model service request duration",
            null),
            
    /**
    * 向量库查询计数
    */
    VECTOR_INDEX_TOTAL_COUNT(
            "everypicfound.vector.index.requests",
            COUNTER,
            "Vector index request count",
            null),
        
    /**
     * 向量库查询耗时
     */
    VECTOR_INDEX_COST_MS(
            "everypicfound.vector.index.duration",
            TIMER,
            "Vector index request duration",
            null),
        
    
    /**
     * 缓存查询计数
     */
    CACHE_ACCESS_TOTAL_COUNT(
            "everypicfound.cache.accesses",
            COUNTER,
            "Cache access count",
            null),
        
    /**
     * 孤儿文件计数
     */
    ORPHAN_FILE_TOTAL_COUNT(
            "everypicfound.orphan.files",
            COUNTER,
            "Detected orphan file count",
            null);


    /**
     * 注册到 Micrometer 的规范指标名。
     */
    private final String metricName;

    /**
     * 指标类型。
     */
    private final MetricType metricType;

    /**
     * 指标描述。
     */
    private final String description;

    /**
     * 基本单位，仅数值分布等指标需要。
     */
    private final String baseUnit;
}
