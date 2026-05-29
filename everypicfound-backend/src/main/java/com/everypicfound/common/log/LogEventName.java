package com.everypicfound.common.log;
public enum LogEventName {

    // 上传开始日志事件。
    UPLOAD_START,

    // 上传成功日志事件。
    UPLOAD_SUCCESS,

    // 上传失败日志事件。
    UPLOAD_FAILED,

    // 孤儿文件记录日志事件。
    ORPHAN_FILE_RECORD,

    // 向量写入成功日志事件。
    VECTOR_UPSERT_SUCCESS,

    // 搜索失败日志事件。
    SEARCH_FAILED,

    // 文件保存成功日志事件。
    FILE_SAVE_SUCCESS,

    // 文件保存失败日志事件。
    FILE_SAVE_FAILED,

    // 文件读取成功日志事件。
    FILE_READ_SUCCESS,

    // 文件读取失败日志事件。
    FILE_READ_FAILED,

    // 文件删除成功日志事件。
    FILE_DELETE_SUCCESS,

    // 文件删除失败日志事件。
    FILE_DELETE_FAILED
}
