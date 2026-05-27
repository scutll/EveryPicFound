package com.everypicfound.common.event;
public class LogEventPublisher implements EventPublisher {

    // 发布通用补偿事件。
    @Override
    public void publish(CommonEvent event) {
        throw new UnsupportedOperationException("TODO");
    }
}
