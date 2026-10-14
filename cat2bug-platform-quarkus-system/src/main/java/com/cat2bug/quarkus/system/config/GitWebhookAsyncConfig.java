package com.cat2bug.quarkus.system.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class GitWebhookAsyncConfig {

    public static final String EXECUTOR_BEAN_NAME = "gitWebhookTaskExecutor";

    @Produces
    @Singleton
    @Named(EXECUTOR_BEAN_NAME)
    public ExecutorService gitWebhookTaskExecutor() {
        AtomicInteger seq = new AtomicInteger();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "git-webhook-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        };
        return Executors.newFixedThreadPool(4, factory);
    }
}
