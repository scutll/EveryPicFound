package com.everypicfound.common.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogContext {

    // 请求 ID，用于问题排查和链路追踪。
    private String requestId;

    // 链路追踪 ID。
    private String traceId;

    // 业务 ID。
    private String bizId;

    // 业务类型，例如 IMAGE_UPLOAD、STORAGE、SEARCH。
    private String bizType;

    // 当前模块。
    private String module;

    // 当前操作。
    private String operation;

    // 当前日志事件名，例如 FILE_SAVE_SUCCESS、UPLOAD_FAILED。
    private String eventName;

    // 当前节点状态，例如 START、SUCCESS、FAILED、SKIPPED。
    private String status;

    // 当前节点耗时，单位毫秒。
    private Long costMs;

    // 错误码，成功时为空。
    private String errorCode;

    // 响应消息。
    private String message;
}
