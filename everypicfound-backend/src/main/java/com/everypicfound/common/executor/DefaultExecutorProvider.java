package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
public class DefaultExecutorProvider implements ExecutorProvider {

    // 统一管理线程池。
    private ThreadPoolManager threadPoolManager;

    // 线程池配置，例如核心线程数、最大线程数、队列长度、拒绝策略。
    private ConcurrencyProperties concurrencyProperties;

    // 根据业务类型返回 Executor；MVP 可统一返回公共线程池。
    @Override
    public Executor getExecutor(ExecutorBizType bizType) {
        throw new UnsupportedOperationException("TODO");
    }
}
