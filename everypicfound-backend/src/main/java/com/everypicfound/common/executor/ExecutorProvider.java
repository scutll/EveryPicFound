package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
public interface ExecutorProvider {

    // 根据业务类型返回 Executor；MVP 可统一返回公共线程池。
    Executor getExecutor(ExecutorBizType bizType);
}
