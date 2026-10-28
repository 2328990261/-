package com.wangrui.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * AI助手线程池配置
 * 定义专门用于AI助手异步任务的线程池Bean
 */
@Configuration
public class AiAssistantConfig {

    /** 创建AI助手专用线程池 */
    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {
        // 1. 新建线程池执行器
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 2. 设置核心线程数
        executor.setCorePoolSize(2);
        // 3. 设置最大线程数
        executor.setMaxPoolSize(4);
        // 4. 设置任务队列容量
        executor.setQueueCapacity(40);
        // 5. 设置线程名前缀
        executor.setThreadNamePrefix("ai-assistant-");
        // 6. 关闭时等待已提交任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        // 7. 设置等待任务完成的超时时间
        executor.setAwaitTerminationSeconds(20);
        return executor;
    }
}