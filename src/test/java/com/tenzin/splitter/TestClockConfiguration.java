package com.tenzin.splitter;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Test doubles shared by every integration test: a controllable clock and a mail catcher. */
@TestConfiguration(proxyBeanMethods = false)
class TestClockConfiguration {

	@Bean
	@Primary
	MutableClock testClock() {
		return new MutableClock();
	}

	@Bean
	@Primary
	CapturingMailSender capturingMailSender() {
		return new CapturingMailSender();
	}

}
