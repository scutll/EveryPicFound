package com.everypicfound.common.log;


/**
 * 日志事件状态。
 *
 * <p>
 * 用于统一日志检索语义，避免不同模块使用任意字符串表示相同状态。
 * </p>
 */
public enum LogStatus {
    
    /**
     * 业务请求被拒绝。
     */
    REJECTED,

    /**
     * 操作执行失败。
     */
    FAILED,

    /**
     * 已进入重试流程。
     */
    RETRYING,

    /**
     * 依赖异常后进入降级流程。
     */
    DEGRADED,

    /**
     * 等待后续补偿或处理。
     */
    WAITING,

    /**
     * 业务资源已被标记为无效。
     */
    INVALIDATED,
                    
    /**
     * 依赖组件已经从异常状态恢复。
     */
    RECOVERED
}