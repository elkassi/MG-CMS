package com.lear.MGCMS.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfiguration {

    @Bean(name = "taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(100); // Set the number of initial threads
        executor.setMaxPoolSize(200); // Set the maximum number of threads
        executor.setQueueCapacity(Integer.MAX_VALUE); // Set the queue capacity for pending tasks
        executor.setThreadNamePrefix("Async-"); // Set a prefix for the thread names
        executor.initialize();
        return executor;
    }
}
