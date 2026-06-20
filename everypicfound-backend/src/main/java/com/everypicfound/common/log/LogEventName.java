package com.everypicfound.common.log;


/**
 * 系统标准日志事件名称。
 *
 * <p>
 * 事件名称表示发生了什么，错误码表示为什么失败。
 * </p>
 */
public enum LogEventName {

    
    /**
     * 未知异常到达系统统一异常处理器。
     */
    COMMON_UNHANDLED_EXCEPTION,

    /**
     * 系统异常发生。
     */
    SYSTEM_EXCEPTION_OCCURRED,

    /**
     * 业务请求由于参数或状态规则被拒绝。
     */
    BUSINESS_REQUEST_REJECTED,

    /**
     * 文件已保存，但元数据入库和补偿删除均失败。
     */
    ORPHAN_FILE_DETECTED,

    /**
     * 异步任务发布失败。
     */
    TASK_PUBLISH_FAILED,

    /**
     * 向量化任务进入重试状态。
     */
    VECTORIZATION_RETRY_SCHEDULED,

    /**
     * 向量化任务达到重试上限或发生不可恢复错误。
     */
    VECTORIZATION_DEAD_FAILED,

    /**
     * 图片文件缺失，图片资产被标记为无效。
     */
    IMAGE_FILE_MISSING,

    /**
     * 缓存发生异常，业务已降级为直接访问数据源。
     */
    CACHE_DEGRADED
    
}
