package com.tenzin.splitter.auth;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param refreshTokenTtl   lifetime of a refresh token
 * @param maxFailedAttempts wrong passwords in a row before the account locks
 * @param lockoutDuration   how long a locked account stays locked
 */
@ConfigurationProperties("app.auth")
public record AuthProperties(
		@DefaultValue("7d") Duration refreshTokenTtl,
		@DefaultValue("5") int maxFailedAttempts,
		@DefaultValue("15m") Duration lockoutDuration) {
}
