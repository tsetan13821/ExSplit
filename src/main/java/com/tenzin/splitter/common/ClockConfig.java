package com.tenzin.splitter.common;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Single source of "now". Anything that checks expiry (tokens, lockout, invites,
 * recurring jobs) injects this, so tests can swap in a fixed clock instead of sleeping.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfig {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

}
