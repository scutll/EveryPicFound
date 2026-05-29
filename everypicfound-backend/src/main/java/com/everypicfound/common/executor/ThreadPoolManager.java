package com.everypicfound.common.executor;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.util.NamedThreadFactory;
import jakarta.annotation.PreDestroy;

//ThreadPoolManager 真正持有和管理线程池，ExecutorProvider向Manager申请并获取线程池再交给申请类
@Component
public class ThreadPoolManager {

    //核心线程数
    private static final int CPU_CORE_SIZE = Runtime.getRuntime().availableProcessors();    

    private static final int COMMON_CORE_POOL_SIZE = Math.max(2, CPU_CORE_SIZE);

    private static final int COMMON_MAX_POOL_SIZE = Math.max(4, CPU_CORE_SIZE * 2);

    private static final int COMMON_QUEUE_CAPACITY = 200;

    private final ThreadPoolExecutor commonExecutor;

    public ThreadPoolManager() {
        this.commonExecutor = new ThreadPoolExecutor(
                COMMON_CORE_POOL_SIZE,
                COMMON_MAX_POOL_SIZE,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(COMMON_QUEUE_CAPACITY),       //任务等待队列
                new NamedThreadFactory("everypicfound-common"),         //线程工厂，创建线程并赋名
                new ThreadPoolExecutor.AbortPolicy());                  //拒绝策略：抛出RejectedExecutionException异常
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


    //定义线程创建方式
    //线程是在线程池中是复用的，所以会有前后运行的任务的线程id相同的情况
    private static class NamedThreadFactory implements ThreadFactory{
        private final String threadNamePrefix;

        private final AtomicInteger threadIndex = new AtomicInteger(1);

        private NamedThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(threadNamePrefix + "-" + threadIndex.getAndIncrement());
            thread.setDaemon(false);        //非守护线程：只要它还在运行，JVM不会直接退出
            return thread;
        }
    }
}
