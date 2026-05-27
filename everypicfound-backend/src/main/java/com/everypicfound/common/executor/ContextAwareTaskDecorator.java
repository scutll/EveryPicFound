package com.everypicfound.common.executor;

public class ContextAwareTaskDecorator {

    //保证异步任务继承 requestId、traceId。
    public Runnable decorate(Runnable runnable) {
        throw new UnsupportedOperationException("TODO");
    }

}
