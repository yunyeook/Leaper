package com.ssafy.spark.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  @Bean(name = "sparkAnalysisExecutor")
  public Executor sparkAnalysisExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(8);
    executor.setMaxPoolSize(16);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("SparkAnalysis-");
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setAwaitTerminationSeconds(60);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return sparkAnalysisExecutor();
  }
}