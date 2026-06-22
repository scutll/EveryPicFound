package com.everypicfound.common.log;


/**
 * 系统标准日志事件名称。
 *
 * <p>
 * eventName 表示发生了什么；
 * errorCode 表示为什么发生。
 * </p>
 */
public enum LogEventName {

    
    /**
     * 未知异常到达统一异常处理器。
     */
    COMMON_UNHANDLED_EXCEPTION,

    /**
     * 系统异常发生。
     */
    SYSTEM_EXCEPTION_OCCURRED,

    /**
     * 业务请求因参数或业务状态规则被拒绝。
     */
    BUSINESS_REQUEST_REJECTED,

    /**
     * 文件已保存，但元数据入库和补偿删除均失败。
     */
    ORPHAN_FILE_DETECTED,

    /**
     * 异步任务发布失败或被线程池拒绝。
     */
    TASK_PUBLISH_FAILED,

    /**
     * 向量化任务进入等待重试状态。
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
    CACHE_DEGRADED,

    /**
     * 向量已经写入向量库，但 READY 状态更新失败，
     * 需要后续一致性补偿。
     */
    VECTOR_READY_COMPENSATION_REQUIRED,

    /**
     * 系统依赖组件由可用状态转为不可用状态。
     */
    SYSTEM_COMPONENT_DOWN,

    /**
     * 系统依赖组件由不可用状态恢复为可用状态。
     */
    SYSTEM_COMPONENT_RECOVERED,

    /**
     * 启动健康检查失败，并可能触发 fail-fast。
     */
    STARTUP_HEALTH_CHECK_FAILED
    
}
