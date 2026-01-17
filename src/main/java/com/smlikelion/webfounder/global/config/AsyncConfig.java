package com.smlikelion.webfounder.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);    // 최소 스레드 수
        executor.setMaxPoolSize(20);     // 최대 허용 스레드 수
        executor.setQueueCapacity(200);  // 대기 작업
        executor.initialize();
        return executor;
    }
}
