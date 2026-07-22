package com.cat2bug.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class GitWebhookAsyncConfig
{
    public static final String EXECUTOR_BEAN_NAME = "gitWebhookTaskExecutor";

    @Bean(name = EXECUTOR_BEAN_NAME)
    public Executor gitWebhookTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("git-webhook-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(256);
        executor.initialize();
        return executor;
    }
}
