package com.example.pickupservice.common.async;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

	private final int corePoolSize;
	private final int maxPoolSize;
	private final int queueCapacity;

	public AsyncConfig(
			@Value("${pickup.async.event.core-pool-size}") int corePoolSize,
			@Value("${pickup.async.event.max-pool-size}") int maxPoolSize,
			@Value("${pickup.async.event.queue-capacity}") int queueCapacity) {
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		this.queueCapacity = queueCapacity;
	}

	@Bean
	public ThreadPoolTaskExecutor eventTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("event-");
		executor.setCorePoolSize(corePoolSize);
		executor.setMaxPoolSize(maxPoolSize);
		executor.setQueueCapacity(queueCapacity);
		return executor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return eventTaskExecutor();
	}

}
