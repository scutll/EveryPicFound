package com.everypicfound.common.executor;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 业务线程池上下文传播装饰器。
 *
 * <p>
 * 在任务提交线程中捕获 RequestContext 和 MDC 快照，
 * 在工作线程执行任务前安装快照，任务结束后恢复工作线程原有状态。
 * </p>
 */
@Component
public class ContextAwareTaskDecorator implements TaskDecorator{

    //保证异步任务继承 requestId、traceId。
    /*
     * TaskDecorator的decorate发生在任务提交线程，可以拿到RequestContextHolder.get()
     * decorator返回包装后的runnable
     * 任务执行线程执行这个runnable，在原本的runnable之前先RequestContextHolder.set(context)
     * 
     */
    @Override
    public Runnable decorate(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable must be bot bull");


        /*
         * decorate 在任务提交线程中执行。
         * 此处必须复制快照，不能保存原始可变对象引用。
         */
        RequestContext capuredRequestContext = copyRequestContext(RequestContextHolder.get());

        Map<String, String> capturedMdcContext = copyMdcContext();


        return () -> {

            /*
             * 保存工作线程执行任务前的状态。
             *
             * 通常线程池工作线程应该没有上下文，但保存并恢复能够兼容：
             * 1. 嵌套任务；
             * 2. 同线程执行；
             * 3. 后续可能采用 CallerRunsPolicy 的情况。
             */
            //保存原线程快照
            RequestContext previousRequestContext = RequestContextHolder.get();

            Map<String, String> previousMdcContext = copyMdcContext();

            try{
                installRequestContext(capuredRequestContext);
                installMdcContext(capturedMdcContext);

                runnable.run();
            } finally {
                restoreRequestContext(previousRequestContext);
                installMdcContext(previousMdcContext);
            }
        };
    }

    /**
     * 创建独立 RequestContext 快照。
     */
    private RequestContext copyRequestContext(RequestContext source) {
        if (source == null) {
            return null;
        }

        return RequestContext.builder()
                .requestId(source.getRequestId())
                .traceId(source.getTraceId())
                .bizId(source.getBizId())
                .module(source.getModule())
                .operation(source.getOperation())
                .build();
    }

    /**
     * 创建独立 MDC Map 快照。
     */
    private Map<String, String> copyMdcContext() {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        if (contextMap == null || contextMap.isEmpty()) {
            return null;
        }

        return new HashMap<>(contextMap);
    }


    /**
     * 安装提交线程的 RequestContext 快照。
     *
     * <p>
     * 每次执行时再次复制，避免包装后的 Runnable 被重复执行时，
     * 多次执行共享同一个可变对象。
     * </p>
     */
    private void installRequestContext(RequestContext capturedContext){
        RequestContextHolder.clear();

        RequestContext contextSnapshot = copyRequestContext(capturedContext);

        if(contextSnapshot != null){
            RequestContextHolder.set(contextSnapshot);
        }
    }




    /**
     * 恢复工作线程原本持有的 RequestContext。
     */
    private void restoreRequestContext(RequestContext previousContext) {
        RequestContextHolder.clear();
        
        if (previousContext != null) {
            RequestContextHolder.set(previousContext);
        }
    }
    
    /**
     * 替换当前线程的完整 MDC Context Map。
     */
    private void installMdcContext(Map<String, String> contextMap) {
        MDC.clear();

        if (contextMap != null && !contextMap.isEmpty()) {
            MDC.setContextMap(new HashMap<>(contextMap));
        }
    }

}
