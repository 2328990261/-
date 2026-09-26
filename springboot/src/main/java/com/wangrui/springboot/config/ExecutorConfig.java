package com.wangrui.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class ExecutorConfig {
    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    @Bean(name = "recommendExecutor")
    public ThreadPoolTaskExecutor recommendExecutor() {
        return buildExecutor(6, 10, 200, "novel-recommend-", new LoggingAbortPolicy("recommend"));
    }

    @Bean(name = "uploadExecutor")
    public ThreadPoolTaskExecutor uploadExecutor() {
        return buildExecutor(2, 4, 20, "novel-upload-", new LoggingAbortPolicy("upload"));
    }

    @Bean(name = "statsExecutor")
    public ThreadPoolTaskExecutor statsExecutor() {
        return buildExecutor(1, 2, 10, "novel-stats-", new LoggingCallerRunsPolicy("stats"));
    }

    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize,
                                                 int maxPoolSize,
                                                 int queueCapacity,
                                                 String threadNamePrefix,
                                                 RejectedExecutionHandler rejectionPolicy) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(rejectionPolicy);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    private static final class LoggingAbortPolicy implements RejectedExecutionHandler {
        private final String workload;

        private LoggingAbortPolicy(String workload) {
            this.workload = workload;
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            log.warn("Executor rejected task workload={} pool={} active={} queue={} thread={}",
                    workload, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size(),
                    Thread.currentThread().getName());
            throw new RejectedExecutionException(workload + " executor is saturated");
        }
    }

    private static final class LoggingCallerRunsPolicy implements RejectedExecutionHandler {
        private final String workload;

        private LoggingCallerRunsPolicy(String workload) {
            this.workload = workload;
        }

        @Override
        public void rejectedExecution(Runnable task, ThreadPoolExecutor executor) {
            log.warn("Executor saturated, caller will run task workload={} pool={} active={} queue={} thread={}",
                    workload, executor.getPoolSize(), executor.getActiveCount(), executor.getQueue().size(),
                    Thread.currentThread().getName());
            if (!executor.isShutdown()) {
                task.run();
            }
        }
    }
}
