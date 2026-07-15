package com.everypicfound.common.event;
public class LogEventStore implements EventStore {

    // 记录通用补偿事件。
    @Override
    public void recordEvent(CommonEvent event) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记事件处理中。
    @Override
    public void markProcessing(String eventId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记事件处理成功。
    @Override
    public void markSuccess(String eventId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 标记事件处理失败。
    @Override
    public void markFailed(String eventId) {
        throw new UnsupportedOperationException("TODO");
    }

    // 记录事件后续重试。
    @Override
    public void retryLater(String eventId) {
        throw new UnsupportedOperationException("TODO");
    }
}
