package com.everypicfound.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommonEvent {

    // 事件 ID。
    private String eventId;

    // 事件类型。
    private String eventType;

    // 事件状态。
    private String eventStatus;

    // 请求 ID。
    private String requestId;

    // 链路追踪 ID。
    private String traceId;

    // 业务 ID。
    private String bizId;

    // 事件来源模块。
    private String sourceModule;

    // 事件载荷。
    private String payload;
}
