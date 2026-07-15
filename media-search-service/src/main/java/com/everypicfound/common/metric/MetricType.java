package com.everypicfound.common.metric;


/**
 * 指标数据类型。
 */
public enum MetricType {
    

    /**
     * 单调递增计数器。
     *
     * <p>
     * 适用于请求次数、失败次数、重试次数等累计值。
     * </p>
     */
    COUNTER,

    /**
     * 耗时分布。
     *
     * <p>
     * 适用于搜索耗时、上传耗时、外部依赖调用耗时。
     * </p>
     */
    TIMER,

    /**
     * 数值分布。
     *
     * <p>
     * 适用于文件大小、召回结果数量等离散样本。
     * </p>
     */
    DISTRIBUTION_SUMMARY,
        
    /**
     * 当前瞬时状态值。
     *
     * <p>
     * 例如线程池活跃线程数、队列长度、组件健康状态。
     * Gauge 由专门的 MetricsBinder 注册，不通过 recordValue 记录。
     * </p>
     */
    GAUGE
}
