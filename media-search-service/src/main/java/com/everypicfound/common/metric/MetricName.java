package com.everypicfound.common.metric;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.everypicfound.common.metric.MetricType.COUNTER;
import static com.everypicfound.common.metric.MetricType.DISTRIBUTION_SUMMARY;
import static com.everypicfound.common.metric.MetricType.GAUGE;
import static com.everypicfound.common.metric.MetricType.TIMER;

/**
 * EveryPicFound 标准指标名称。
 *
 * <p>
 * 指标名称只描述“统计的对象是什么”，
 * operation、stage、result、reason 等变化维度通过 MetricTag 表达。
 * </p>
 *
 * <p>
 * 禁止为 save、read、delete 等不同操作分别创建结构相同的指标，
 * 避免指标名称膨胀和后续查询口径不一致。
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum MetricName {

        /*
         * ============================================================
         * 1. 图片上传链路
         * ============================================================
         */

        /**
         * 图片上传请求次数。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        IMAGE_UPLOAD_REQUESTS(
                        "everypicfound.image.upload.requests",
                        COUNTER,
                        "Image upload request count",
                        null),

        /**
         * 图片上传完整用例耗时。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        IMAGE_UPLOAD_DURATION(
                        "everypicfound.image.upload.duration",
                        TIMER,
                        "Image upload request duration",
                        null),

        /**
         * 图片上传内部阶段耗时。
         *
         * <p>
         * 固定标签：stage、result。
         * stage 可取 validate、metadata_extract、hash、
         * duplicate_check、storage_save、metadata_save、publish。
         * </p>
         */
        IMAGE_UPLOAD_STAGE_DURATION(
                        "everypicfound.image.upload.stage.duration",
                        TIMER,
                        "Image upload stage duration",
                        null),

        /**
         * 上传图片文件大小分布。
         */
        IMAGE_UPLOAD_FILE_SIZE(
                        "everypicfound.image.upload.file.size",
                        DISTRIBUTION_SUMMARY,
                        "Uploaded image file size distribution",
                        "bytes"),

        /**
         * 上传请求被业务规则拒绝的次数。
         *
         * <p>
         * 固定标签：reason。
         * </p>
         */
        IMAGE_UPLOAD_REJECTIONS(
                        "everypicfound.image.upload.rejections",
                        COUNTER,
                        "Image upload rejection count",
                        null),

        /**
         * 重复图片上传次数。
         */
        IMAGE_UPLOAD_DUPLICATES(
                        "everypicfound.image.upload.duplicates",
                        COUNTER,
                        "Duplicate image upload count",
                        null),

        /**
         * 上传补偿操作次数。
         *
         * <p>
         * 固定标签：result、reason。
         * 用于文件保存成功但元数据入库失败后的删除补偿。
         * </p>
         */
        IMAGE_UPLOAD_COMPENSATIONS(
                        "everypicfound.image.upload.compensations",
                        COUNTER,
                        "Image upload compensation count",
                        null),

        /**
         * 检测到的孤儿文件次数。
         */
        ORPHAN_FILES(
                        "everypicfound.orphan.files",
                        COUNTER,
                        "Detected orphan file count",
                        null),

        /*
         * ============================================================
         * 2. 图片文件访问链路
         * ============================================================
         */

        /**
         * 图片文件 HTTP 访问次数。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        IMAGE_ACCESS_REQUESTS(
                        "everypicfound.image.access.requests",
                        COUNTER,
                        "Image access request count",
                        null),

        /**
         * 图片文件 HTTP 访问完整耗时。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        IMAGE_ACCESS_DURATION(
                        "everypicfound.image.access.duration",
                        TIMER,
                        "Image access request duration",
                        null),

        /**
         * 图片响应文件大小分布。
         */
        IMAGE_ACCESS_RESPONSE_SIZE(
                        "everypicfound.image.access.response.size",
                        DISTRIBUTION_SUMMARY,
                        "Image access response size distribution",
                        "bytes"),

        /**
         * StreamingResponseBody 写出阶段耗时。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        IMAGE_ACCESS_STREAM_DURATION(
                        "everypicfound.image.access.stream.duration",
                        TIMER,
                        "Image response streaming duration",
                        null),

        /**
         * 实际向客户端输出的字节数。
         */
        IMAGE_ACCESS_TRANSFERRED_BYTES(
                        "everypicfound.image.access.transferred",
                        DISTRIBUTION_SUMMARY,
                        "Image response transferred byte distribution",
                        "bytes"),

        /*
         * ============================================================
         * 3. 搜索统一链路
         * ============================================================
         */

        /**
         * 搜索请求次数。
         *
         * <p>
         * 固定标签：search_type、result、cache_result。
         * </p>
         */
        SEARCH_REQUESTS(
                        "everypicfound.search.requests",
                        COUNTER,
                        "Search request count",
                        null),

        /**
         * 搜索完整用例耗时。
         *
         * <p>
         * 固定标签：search_type、result、cache_result。
         * </p>
         */
        SEARCH_DURATION(
                        "everypicfound.search.duration",
                        TIMER,
                        "Search request duration",
                        null),

        /**
         * 搜索管线阶段耗时。
         *
         * <p>
         * 固定标签：search_type、stage、result。
         * </p>
         */
        SEARCH_STAGE_DURATION(
                        "everypicfound.search.stage.duration",
                        TIMER,
                        "Search pipeline stage duration",
                        null),

        /**
         * 搜索请求被业务规则拒绝的次数。
         *
         * <p>
         * 固定标签：search_type、reason。
         * </p>
         */
        SEARCH_REJECTIONS(
                        "everypicfound.search.rejections",
                        COUNTER,
                        "Search rejection count",
                        null),

        /**
         * 单次搜索最终返回结果数量分布。
         *
         * <p>
         * 固定标签：search_type。
         * </p>
         */
        SEARCH_RESULT_COUNT(
                        "everypicfound.search.results",
                        DISTRIBUTION_SUMMARY,
                        "Search result count distribution",
                        null),

        /**
         * 搜索返回空结果的次数。
         *
         * <p>
         * 固定标签：search_type。
         * </p>
         */
        SEARCH_EMPTY_RESULTS(
                        "everypicfound.search.empty",
                        COUNTER,
                        "Empty search result count",
                        null),

        /**
         * 搜索管线各阶段处理条目数量。
         *
         * <p>
         * 固定标签：search_type、stage。
         * stage 可取 recall、backfill_requested、
         * backfill_returned、filter_output。
         * </p>
         */
        SEARCH_PIPELINE_ITEM_COUNT(
                        "everypicfound.search.pipeline.items",
                        DISTRIBUTION_SUMMARY,
                        "Search pipeline item count distribution",
                        null),

        /**
         * 搜索结果过滤数量。
         *
         * <p>
         * 固定标签：search_type、reason。
         * reason 可取 orphan_vector、invalid_image。
         * </p>
         */
        SEARCH_FILTERED_ITEM_COUNT(
                        "everypicfound.search.filtered.items",
                        DISTRIBUTION_SUMMARY,
                        "Filtered search item count distribution",
                        null),

        /**
         * Over-fetch 计算出的 topN 分布。
         *
         * <p>
         * 固定标签：search_type。
         * </p>
         */
        SEARCH_TOP_N(
                        "everypicfound.search.topn",
                        DISTRIBUTION_SUMMARY,
                        "Search topN distribution",
                        null),

        /**
         * 搜索查询图片大小。
         *
         * <p>
         * 固定标签：search_type。
         * </p>
         */
        SEARCH_QUERY_IMAGE_SIZE(
                        "everypicfound.search.query.image.size",
                        DISTRIBUTION_SUMMARY,
                        "Search query image size distribution",
                        "bytes"),

        /**
         * 搜索查询文本长度。
         *
         * <p>
         * 固定标签：search_type。
         * </p>
         */
        SEARCH_QUERY_TEXT_LENGTH(
                        "everypicfound.search.query.text.length",
                        DISTRIBUTION_SUMMARY,
                        "Search query text length distribution",
                        null),

        /**
         * 查询向量非法次数。
         *
         * <p>
         * 固定标签：search_type、reason。
         * reason 可取 empty、dimension_mismatch。
         * </p>
         */
        SEARCH_EMBEDDING_INVALID(
                        "everypicfound.search.embedding.invalid",
                        COUNTER,
                        "Invalid search embedding count",
                        null),

        /*
         * ============================================================
         * 4. 向量化任务发布与执行
         * ============================================================
         */

        /**
         * 向量化任务发布次数。
         *
         * <p>
         * 固定标签：source、result。
         * </p>
         */
        VECTORIZATION_PUBLISH_REQUESTS(
                        "everypicfound.vectorization.publish.requests",
                        COUNTER,
                        "Vectorization task publish count",
                        null),

        /**
         * 向量化任务发布耗时。
         *
         * <p>
         * 固定标签：source、result。
         * </p>
         */
        VECTORIZATION_PUBLISH_DURATION(
                        "everypicfound.vectorization.publish.duration",
                        TIMER,
                        "Vectorization task publish duration",
                        null),

        /**
         * 图片向量化任务执行次数。
         *
         * <p>
         * 固定标签：source、result、reason。
         * </p>
         */
        VECTORIZATION_TASKS(
                        "everypicfound.vectorization.tasks",
                        COUNTER,
                        "Image vectorization task count",
                        null),

        /**
         * 图片向量化完整任务耗时。
         *
         * <p>
         * 固定标签：source、result。
         * </p>
         */
        VECTORIZATION_DURATION(
                        "everypicfound.vectorization.duration",
                        TIMER,
                        "Image vectorization task duration",
                        null),

        /**
         * 图片向量化内部阶段耗时。
         *
         * <p>
         * 固定标签：stage、result。
         * stage 可取 asset_query、mark_processing、storage_check、
         * storage_read、resolve_collection、model_inference、
         * dimension_validate、vector_upsert、mark_ready。
         * </p>
         */
        VECTORIZATION_STAGE_DURATION(
                        "everypicfound.vectorization.stage.duration",
                        TIMER,
                        "Vectorization processing stage duration",
                        null),

        /**
         * 向量化任务进入重试状态的次数。
         *
         * <p>
         * 固定标签：reason。
         * </p>
         */
        VECTORIZATION_RETRIES(
                        "everypicfound.vectorization.retries",
                        COUNTER,
                        "Image vectorization retry count",
                        null),

        /**
         * 向量维度不匹配次数。
         *
         * <p>
         * 固定标签：source。
         * </p>
         */
        VECTORIZATION_DIMENSION_MISMATCH(
                        "everypicfound.vectorization.dimension.mismatch",
                        COUNTER,
                        "Vectorization dimension mismatch count",
                        null),

        /**
         * 向量已经写入，但 READY 状态更新失败的次数。
         */
        VECTORIZATION_READY_UPDATE_FAILURES(
                        "everypicfound.vectorization.ready.update.failures",
                        COUNTER,
                        "Vectorization READY status update failure count",
                        null),

        /*
         * ============================================================
         * 5. 文件存储
         * ============================================================
         */

        /**
         * Storage 操作次数。
         *
         * <p>
         * 固定标签：operation、result。
         * operation 可取 save、read、delete、exists。
         * </p>
         */
        STORAGE_OPERATIONS(
                        "everypicfound.storage.operations",
                        COUNTER,
                        "Storage operation count",
                        null),

        /**
         * Storage 操作耗时。
         *
         * <p>
         * 固定标签：operation、result。
         * </p>
         */
        STORAGE_DURATION(
                        "everypicfound.storage.duration",
                        TIMER,
                        "Storage operation duration",
                        null),

        /**
         * Storage 处理的文件大小分布。
         *
         * <p>
         * 固定标签：operation。
         * </p>
         */
        STORAGE_FILE_SIZE(
                        "everypicfound.storage.file.size",
                        DISTRIBUTION_SUMMARY,
                        "Storage file size distribution",
                        "bytes"),

        /**
         * 文件不存在次数。
         *
         * <p>
         * 固定标签：source。
         * </p>
         */
        STORAGE_MISSING_FILES(
                        "everypicfound.storage.missing",
                        COUNTER,
                        "Missing storage file count",
                        null),

        /*
         * ============================================================
         * 6. 图片资产 Repository
         * ============================================================
         */

        /**
         * 图片资产 Repository 操作次数。
         *
         * <p>
         * 固定标签：operation、result。
         * operation 可取 save、find_by_id、find_by_ids、
         * exists_by_hash、update_status。
         * </p>
         */
        IMAGE_ASSET_REPOSITORY_OPERATIONS(
                        "everypicfound.image.asset.repository.operations",
                        COUNTER,
                        "Image asset repository operation count",
                        null),

        /**
         * 图片资产 Repository 操作耗时。
         *
         * <p>
         * 固定标签：operation、result。
         * </p>
         */
        IMAGE_ASSET_REPOSITORY_DURATION(
                        "everypicfound.image.asset.repository.duration",
                        TIMER,
                        "Image asset repository operation duration",
                        null),

        /*
         * ============================================================
         * 7. 业务缓存
         * ============================================================
         */

        /**
         * 业务缓存操作次数。
         *
         * <p>
         * 固定标签：cache_name、operation、result。
         * cache_name 可取 search_result、text_vector。
         * </p>
         */
        CACHE_OPERATIONS(
                        "everypicfound.cache.operations",
                        COUNTER,
                        "Business cache operation count",
                        null),

        /**
         * 业务缓存操作耗时。
         *
         * <p>
         * 固定标签：cache_name、operation、result。
         * </p>
         */
        CACHE_DURATION(
                        "everypicfound.cache.duration",
                        TIMER,
                        "Business cache operation duration",
                        null),

        /*
         * ============================================================
         * 8. Redis 基础设施
         * ============================================================
         */

        /**
         * Redis 操作次数。
         *
         * <p>
         * 固定标签：operation、result。
         * operation 可取 get、put、evict、exists。
         * </p>
         */
        REDIS_OPERATIONS(
                        "everypicfound.redis.operations",
                        COUNTER,
                        "Redis operation count",
                        null),

        /**
         * Redis 操作耗时。
         *
         * <p>
         * 固定标签：operation、result。
         * </p>
         */
        REDIS_DURATION(
                        "everypicfound.redis.duration",
                        TIMER,
                        "Redis operation duration",
                        null),

        /*
         * ============================================================
         * 9. Java 模型客户端
         * ============================================================
         */

        /**
         * 高层模型服务调用次数。
         *
         * <p>
         * 固定标签：operation、source、result。
         * operation 可取 vectorize_image、vectorize_text、health。
         * </p>
         */
        MODEL_CLIENT_REQUESTS(
                        "everypicfound.model.client.requests",
                        COUNTER,
                        "Model service client request count",
                        null),

        /**
         * 高层模型服务调用耗时。
         *
         * <p>
         * 固定标签：operation、source、result。
         * </p>
         */
        MODEL_CLIENT_DURATION(
                        "everypicfound.model.client.duration",
                        TIMER,
                        "Model service client request duration",
                        null),

        /**
         * 调用 Python 服务的实际 HTTP 请求次数。
         *
         * <p>
         * 固定标签：endpoint、result。
         * </p>
         */
        MODEL_HTTP_REQUESTS(
                        "everypicfound.model.http.requests",
                        COUNTER,
                        "Model service HTTP request count",
                        null),

        /**
         * 调用 Python 服务的实际 HTTP 请求耗时。
         *
         * <p>
         * 固定标签：endpoint、result。
         * </p>
         */
        MODEL_HTTP_DURATION(
                        "everypicfound.model.http.duration",
                        TIMER,
                        "Model service HTTP request duration",
                        null),

        /**
         * 模型服务响应不合法的次数。
         *
         * <p>
         * 固定标签：operation、reason。
         * </p>
         */
        MODEL_RESPONSE_INVALID(
                        "everypicfound.model.response.invalid",
                        COUNTER,
                        "Invalid model service response count",
                        null),

        /*
         * ============================================================
         * 10. Qdrant 向量数据库
         * ============================================================
         */

        /**
         * 向量数据库操作次数。
         *
         * <p>
         * 固定标签：operation、result。
         * operation 可取 search、upsert、delete、exists。
         * </p>
         */
        VECTOR_INDEX_REQUESTS(
                        "everypicfound.vector.index.requests",
                        COUNTER,
                        "Vector index request count",
                        null),

        /**
         * 向量数据库操作耗时。
         *
         * <p>
         * 固定标签：operation、result。
         * </p>
         */
        VECTOR_INDEX_DURATION(
                        "everypicfound.vector.index.duration",
                        TIMER,
                        "Vector index request duration",
                        null),

        /**
         * Qdrant 单次检索返回的结果数量。
         */
        VECTOR_INDEX_SEARCH_RESULT_COUNT(
                        "everypicfound.vector.index.search.results",
                        DISTRIBUTION_SUMMARY,
                        "Vector index search result count distribution",
                        null),

        /*
         * ============================================================
         * 11. Active Collection
         * ============================================================
         */

        /**
         * Active Collection 解析次数。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        VECTOR_COLLECTION_RESOLVE_REQUESTS(
                        "everypicfound.vector.collection.resolve.requests",
                        COUNTER,
                        "Active vector collection resolve count",
                        null),

        /**
         * Active Collection 解析耗时。
         *
         * <p>
         * 固定标签：result。
         * </p>
         */
        VECTOR_COLLECTION_RESOLVE_DURATION(
                        "everypicfound.vector.collection.resolve.duration",
                        TIMER,
                        "Active vector collection resolve duration",
                        null),

        /*
         * ============================================================
         * 12. 系统健康检查
         * ============================================================
         */

        /**
         * 系统整体健康检查次数。
         *
         * <p>
         * 固定标签：source、result。
         * </p>
         */
        HEALTH_CHECKS(
                        "everypicfound.health.checks",
                        COUNTER,
                        "System health check count",
                        null),

        /**
         * 系统整体健康检查耗时。
         *
         * <p>
         * 固定标签：source、result。
         * </p>
         */
        HEALTH_DURATION(
                        "everypicfound.health.duration",
                        TIMER,
                        "System health check duration",
                        null),

        /**
         * 单个依赖组件健康检查次数。
         *
         * <p>
         * 固定标签：component、result。
         * </p>
         */
        HEALTH_COMPONENT_CHECKS(
                        "everypicfound.health.component.checks",
                        COUNTER,
                        "Component health check count",
                        null),

        /**
         * 单个依赖组件健康检查耗时。
         *
         * <p>
         * 固定标签：component、result。
         * </p>
         */
        HEALTH_COMPONENT_DURATION(
                        "everypicfound.health.component.duration",
                        TIMER,
                        "Component health check duration",
                        null),

        /**
         * 组件当前健康状态。
         *
         * <p>
         * 固定标签：component。
         * 1 表示 UP，0 表示 DOWN。
         * </p>
         */
        HEALTH_COMPONENT_STATUS(
                        "everypicfound.health.component.status",
                        GAUGE,
                        "Current component health status",
                        null),

        /*
         * ============================================================
         * 13. 业务线程池
         * ============================================================
         */

        /**
         * 线程池拒绝任务次数。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_REJECTIONS(
                        "everypicfound.executor.rejected",
                        COUNTER,
                        "Executor rejected task count",
                        null),

        /**
         * 当前活跃线程数。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_ACTIVE(
                        "everypicfound.executor.active",
                        GAUGE,
                        "Current executor active thread count",
                        null),

        /**
         * 当前线程池线程数量。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_POOL_SIZE(
                        "everypicfound.executor.pool.size",
                        GAUGE,
                        "Current executor pool size",
                        null),

        /**
         * 线程池最大线程数量。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_MAX_POOL_SIZE(
                        "everypicfound.executor.pool.max",
                        GAUGE,
                        "Executor maximum pool size",
                        null),

        /**
         * 当前等待队列任务数量。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_QUEUE_SIZE(
                        "everypicfound.executor.queue.size",
                        GAUGE,
                        "Current executor queue size",
                        null),

        /**
         * 当前等待队列剩余容量。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_QUEUE_REMAINING_CAPACITY(
                        "everypicfound.executor.queue.remaining",
                        GAUGE,
                        "Current executor queue remaining capacity",
                        null),

        /**
         * 线程池已完成任务数量。
         *
         * <p>
         * 固定标签：executor。
         * </p>
         */
        EXECUTOR_COMPLETED_TASKS(
                        "everypicfound.executor.completed",
                        GAUGE,
                        "Executor completed task count",
                        null);

        /**
         * 注册到 Micrometer 的规范指标名称。
         */
        private final String metricName;

        /**
         * 指标类型。
         */
        private final MetricType metricType;

        /**
         * 指标说明。
         */
        private final String description;

        /**
         * 基本单位。
         *
         * <p>
         * Timer 不需要在此声明毫秒或秒；
         * Micrometer 会按照后端系统要求进行单位转换。
         * </p>
         */
        private final String baseUnit;
}