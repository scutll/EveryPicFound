package com.everypicfound.common.executor;

import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import com.everypicfound.common.context.RequestContext;
import com.everypicfound.common.context.RequestContextHolder;


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
        RequestContext context = RequestContextHolder.get();

        return () -> {
            if (context != null) {
                RequestContextHolder.set(context);
            }

            try{
                runnable.run();
            } finally {
                RequestContextHolder.clear();
            }
        };
    }

}
