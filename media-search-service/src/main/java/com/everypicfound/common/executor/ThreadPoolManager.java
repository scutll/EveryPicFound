package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import jakarta.annotation.PreDestroy;

//ThreadPoolManager 真正持有和管理线程池，ExecutorProvider向Manager申请并获取线程池再交给申请类
/**
 * 系统线程池统一管理器。
 *
 * <p>
 * 当前阶段，各业务类型共用同一个公共线程池。
 * 后续需要进行线程池隔离时，可根据 ExecutorBizType
 * 返回不同的 ThreadPoolTaskExecutor。
 * </p>
 */
@Component
public class ThreadPoolManager {

    /**
     * 当前机器可用 CPU 核心数。
     */
    private static final int CPU_CORE_SIZE = Runtime.getRuntime().availableProcessors();

    /**
     * 公共线程池核心线程数。
     */
    private static final int COMMON_CORE_POOL_SIZE = Math.max(2, CPU_CORE_SIZE);

    /**
     * 公共线程池最大线程数。
     */
    private static final int COMMON_MAX_POOL_SIZE = Math.max(4, CPU_CORE_SIZE * 2);

    /**
     * 公共线程池等待队列容量。
     */
    private static final int COMMON_QUEUE_CAPACITY = 200;

    /**
     * 线程空闲存活时间，单位秒。
     */
    private static final int COMMON_KEEP_ALIVE_SECONDS = 60;

    /**
     * 公共线程池名称。
     *
     * <p>
     * 该名称同时作为指标 executor 标签值，
     * 不包含 requestId、业务 ID 等高基数内容。
     * </p>
     */
    public static final String COMMON_EXECUTOR_NAME = "common";


    private final ThreadPoolTaskExecutor commonExecutor;

    public ThreadPoolManager(ContextAwareTaskDecorator contextAwareTaskDecorator) {
        this.commonExecutor = buildExecutor("everypicfound-common-",COMMON_CORE_POOL_SIZE, COMMON_MAX_POOL_SIZE, COMMON_QUEUE_CAPACITY, contextAwareTaskDecorator);
    }
    



    /**
     * 获取公共 Executor。
     *
     * @return 公共线程池
     */
    public Executor getCommonExecutor() {
        return commonExecutor;
    }


    /**
     * 根据业务类型获取线程池。
     *
     * <p>
     * 当前所有业务共用公共线程池，因此 bizType 暂未影响返回结果。
     * 后续实现业务线程池隔离时，可以在此处按照业务类型路由。
     * </p>
     *
     * @param bizType 业务线程池类型
     * @return 对应线程池
     */
    public Executor getExecutorByType(ExecutorBizType bizType) {
        return commonExecutor;
    }

    /**
     * 向线程池指标绑定器暴露 Spring 线程池对象。
     *
     * <p>
     * 业务代码不应直接调用该方法提交任务；
     * 业务仍然通过 ExecutorProvider 获取 Executor。
     * 该方法只用于线程池管理和实时指标采集。
     * </p>
     *
     * @return 公共 ThreadPoolTaskExecutor
     */
    public ThreadPoolTaskExecutor getCommonTaskExecutor() {
        return commonExecutor;
    }

    /**
     * 获取公共线程池底层 JDK ThreadPoolExecutor。
     *
     * <p>
     * ExecutorGaugeBinder 需要读取活跃线程数、
     * 队列长度和已完成任务数量等实时状态。
     * </p>
     *
     * @return 底层 ThreadPoolExecutor
     */
    public ThreadPoolExecutor getCommonThreadPoolExecutor() {
        return commonExecutor.getThreadPoolExecutor();
    }


    /**
     * Spring 容器关闭时释放线程池。
     */
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
        executor.setKeepAliveSeconds(COMMON_KEEP_ALIVE_SECONDS);

        /*
         * 负责将 RequestContext 等上下文从提交任务线程
         * 复制到实际执行任务的线程。
         */
        executor.setTaskDecorator(contextAwareTaskDecorator);

        /*
         * 队列和最大线程都已满时直接抛出
         * RejectedExecutionException。
         *
         * Publisher 捕获该异常后：
         * 1. 返回发布失败结果；
         * 2. 记录 TASK_PUBLISH_FAILED；
         * 3. 增加 EXECUTOR_REJECTIONS 指标。
         */
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        executor.initialize();

        return executor;
    }
}
