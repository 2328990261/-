package com.wangrui.springboot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 * 定义推荐、上传、统计三类异步任务专用线程池，并配套拒绝策略
 */
@Configuration
public class ExecutorConfig {
    private static final Logger log = LoggerFactory.getLogger(ExecutorConfig.class);

    /** 推荐任务线程池 */
    @Bean(name = "recommendExecutor")
    public ThreadPoolTaskExecutor recommendExecutor() {
        return buildExecutor(6, 10, 200, "novel-recommend-", new LoggingAbortPolicy("recommend"));
    }

    /** 上传任务线程池 */
    @Bean(name = "uploadExecutor")
    public ThreadPoolTaskExecutor uploadExecutor() {
        return buildExecutor(2, 4, 20, "novel-upload-", new LoggingAbortPolicy("upload"));
    }

    /** 统计数据线程池 */
    @Bean(name = "statsExecutor")
    public ThreadPoolTaskExecutor statsExecutor() {
        return buildExecutor(1, 2, 10, "novel-stats-", new LoggingCallerRunsPolicy("stats"));
    }

    /** 通用构建线程池：配置核心/最大线程数、队列容量、线程名前缀与拒绝策略 */
    private ThreadPoolTaskExecutor buildExecutor(int corePoolSize,
                                                 int maxPoolSize,
                                                 int queueCapacity,
                                                 String threadNamePrefix,
                                                 RejectedExecutionHandler rejectionPolicy) {
        // 1. 创建线程池执行器
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 2. 设置核心线程数
        executor.setCorePoolSize(corePoolSize);
        // 3. 设置最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        // 4. 设置任务队列容量
        executor.setQueueCapacity(queueCapacity);
        // 5. 设置非核心线程空闲存活时间
        executor.setKeepAliveSeconds(60);
        // 6. 设置线程名前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        // 7. 设置队列满时的拒绝策略
        executor.setRejectedExecutionHandler(rejectionPolicy);
        // 8. 关闭时等待任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 9. 设置关闭等待超时时间
        executor.setAwaitTerminationSeconds(30);
        return executor;
    }

    /** 拒绝策略：记录告警日志后抛出异常，用于推荐/上传这种不应阻塞主流程的任务 */
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

    /** 拒绝策略：队列满时由调用线程执行任务，保证关键统计不漏算 */
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
            // 只有在线程池未关闭时，才让调用线程直接执行任务
            if (!executor.isShutdown()) {
                task.run();
            }
        }
    }
}