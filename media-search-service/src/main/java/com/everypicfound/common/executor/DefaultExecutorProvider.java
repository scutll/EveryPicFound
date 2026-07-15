package com.everypicfound.common.executor;

import java.util.concurrent.Executor;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultExecutorProvider implements ExecutorProvider {

    private final ThreadPoolManager threadPoolManager;


    // 根据业务类型返回 Executor；MVP 可统一返回公共线程池。
    @Override
    public Executor getExecutor(ExecutorBizType bizType) {
        if (bizType == null) {
            return threadPoolManager.getCommonExecutor();
        }

        return threadPoolManager.getExecutorByType(bizType);
    }
}
