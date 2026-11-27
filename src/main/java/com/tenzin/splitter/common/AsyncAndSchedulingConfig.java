package com.tenzin.splitter.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Async uses Spring Boot's auto-configured task executor. */
@Configuration(proxyBeanMethods = false)
@EnableAsync
public class AsyncAndSchedulingConfig {

	/**
	 * Cron jobs can be switched off with {@code app.scheduling.enabled=false}; tests do so
	 * and call the job directly with a controlled clock.
	 */
	@Configuration(proxyBeanMethods = false)
	@EnableScheduling
	@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
	static class SchedulingConfig {

	}

}
