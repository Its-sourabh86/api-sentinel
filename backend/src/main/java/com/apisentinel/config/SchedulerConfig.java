package com.apisentinel.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(SchedulerProperties.class)
@ConditionalOnProperty(name = "sentinel.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig {

    @Bean(destroyMethod = "shutdown")
    public ThreadPoolExecutor checkWorkerPool(SchedulerProperties props) {
        AtomicInteger counter = new AtomicInteger(1);
        return new ThreadPoolExecutor(
                props.workerThreads(), props.workerThreads(),
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(props.queueCapacity()),
                runnable -> new Thread(runnable, "check-worker-" + counter.getAndIncrement()),
                new ThreadPoolExecutor.AbortPolicy());
    }
}
