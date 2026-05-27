package com.everypicfound.common.event;
public interface EventStore {

    // 记录通用补偿事件。
    void recordEvent(CommonEvent event);

    // 标记事件处理中。
    void markProcessing(String eventId);

    // 标记事件处理成功。
    void markSuccess(String eventId);

    // 标记事件处理失败。
    void markFailed(String eventId);

    // 记录事件后续重试。
    void retryLater(String eventId);
}
