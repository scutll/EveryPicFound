package com.everypicfound.common.event;
public interface EventPublisher {

    // 发布通用补偿事件。
    void publish(CommonEvent event);
}
