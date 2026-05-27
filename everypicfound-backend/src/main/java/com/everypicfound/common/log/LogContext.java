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

    // 当前模块。
    private String module;

    // 当前操作。
    private String operation;

    // 响应消息。
    private String message;
}
