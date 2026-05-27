package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
public class ThreadPoolManager {

    // 获取公共线程池。
    public Executor getCommonExecutor() {
        throw new UnsupportedOperationException("TODO");
    }

    // 根据业务类型获取线程池。
    public Executor getExecutorByType(ExecutorBizType bizType) {
        throw new UnsupportedOperationException("TODO");
    }
}
