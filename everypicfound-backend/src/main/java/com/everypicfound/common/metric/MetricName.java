package com.everypicfound.common.metric;
public enum MetricName {

    // 搜索总次数指标。
    SEARCH_TOTAL_COUNT,

    // 搜索耗时指标。
    SEARCH_COST_MS,

    // 上传总次数指标。
    UPLOAD_TOTAL_COUNT,

    // 向量化耗时指标。
    VECTORIZATION_COST_MS,

    // 文件保存耗时指标。
    FILE_SAVE_DURATION_MS,

    // 文件保存失败次数指标。
    FILE_SAVE_FAILED_COUNT,

    // 已保存文件大小指标。
    STORED_FILE_SIZE_BYTES,

    // 文件读取耗时指标。
    FILE_READ_DURATION_MS,

    // 文件读取失败次数指标。
    FILE_READ_FAILED_COUNT,

    // 文件缺失次数指标。
    FILE_MISSING_COUNT,

    // 文件删除耗时指标。
    FILE_DELETE_DURATION_MS,

    // 文件删除失败次数指标。
    FILE_DELETE_FAILED_COUNT,

    //上传耗时指标
    UPLOAD_COST_MS
}
