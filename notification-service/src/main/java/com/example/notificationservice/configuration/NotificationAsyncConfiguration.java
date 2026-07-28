package com.example.notificationservice.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@Slf4j
public class NotificationAsyncConfiguration {

    public static final String MAIL_EXECUTOR = "notificationMailExecutor";

    @Bean(name = MAIL_EXECUTOR)
    Executor notificationMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notification-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.setRejectedExecutionHandler((task, threadPool) -> {
            if (threadPool.isShutdown()) {
                log.warn("Email task rejected because notification mail executor is shutting down");
                return;
            }

            log.warn("Notification mail executor is saturated; running email task in the caller thread");
            task.run();
        });
        executor.initialize();
        return executor;
    }
}
