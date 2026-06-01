package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PreDestroy;

//ThreadPoolManager 真正持有和管理线程池，ExecutorProvider向Manager申请并获取线程池再交给申请类
@Component
public class ThreadPoolManager {

    //核心线程数
    private static final int CPU_CORE_SIZE = Runtime.getRuntime().availableProcessors();    

    private static final int COMMON_CORE_POOL_SIZE = Math.max(2, CPU_CORE_SIZE);

    private static final int COMMON_MAX_POOL_SIZE = Math.max(4, CPU_CORE_SIZE * 2);

    private static final int COMMON_QUEUE_CAPACITY = 200;


    private final ThreadPoolTaskExecutor commonExecutor;

    public ThreadPoolManager(ContextAwareTaskDecorator contextAwareTaskDecorator) {
        this.commonExecutor = buildExecutor("everypicfound-common-",COMMON_CORE_POOL_SIZE, COMMON_MAX_POOL_SIZE, COMMON_QUEUE_CAPACITY, contextAwareTaskDecorator);
    }
    



    // 获取公共线程池。
    public Executor getCommonExecutor() {
        return commonExecutor;
    }

    // 根据业务类型获取线程池。
    // 当前实现所有业务类先公用公共线程池
    public Executor getExecutorByType(ExecutorBizType bizType) {
        return commonExecutor;
    }


    //spring 服务关闭的时候释放线程池
    @PreDestroy
    public void shutdown() {
        commonExecutor.shutdown();
    }


    private ThreadPoolTaskExecutor buildExecutor(String threadNamePrefix, int corePoolSize, int maxPoolSize,
            int queueCapacity, ContextAwareTaskDecorator contextAwareTaskDecorator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setTaskDecorator(contextAwareTaskDecorator);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        return executor;
    }
}
