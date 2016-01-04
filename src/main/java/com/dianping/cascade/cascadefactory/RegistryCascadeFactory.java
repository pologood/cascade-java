package com.dianping.cascade.cascadefactory;

import com.dianping.cascade.*;
import com.dianping.cascade.invoker.field.ExceptionHandler;
import com.dianping.cascade.invoker.field.PropsSupport;
import com.dianping.cascade.invoker.field.RegistryFieldInvoker;
import com.dianping.cascade.reducer.ParallelReducer;
import com.dianping.cascade.reducer.SerialReducer;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by yangjie on 12/15/15.
 */
@AllArgsConstructor
public class RegistryCascadeFactory implements CascadeFactory {
    private Registry registry;
    private CascadeFactoryConfig config;

    @Override
    public Cascade create() {
        final FieldInvoker fieldInvoker = new ExceptionHandler(
                new PropsSupport(
                    new RegistryFieldInvoker(registry)
            )
        );

        int threadCount = config.getThreadCount();
        Reducer reducer;

        if (threadCount> 1) {
            BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<Runnable>(threadCount);

            reducer = new ParallelReducer(fieldInvoker, new ThreadPoolExecutor(
                    threadCount,
                    threadCount,
                    0L,
                    TimeUnit.MILLISECONDS,
                    taskQueue, // 额外接受1倍的task，拍脑袋定的，可以优化
                    new ThreadFactoryBuilder().setNameFormat("cascade-%d").build(),
                    new ThreadPoolExecutor.CallerRunsPolicy()
            ), taskQueue);

        } else {
            reducer = new SerialReducer(fieldInvoker);
        }

        final Reducer finalReducer = reducer;

        return new Cascade() {
            @Override
            public Map process(List<Field> fields, Map contextParams) {
                return finalReducer.reduce(fields, ContextParams.create(contextParams));
            }
        };
    }
}
