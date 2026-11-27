package com.tenzin.splitter.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param secret         Base64-encoded HMAC key, at least 256 bits. Supplied via JWT_SECRET.
 * @param accessTokenTtl how long an access token stays valid
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, @DefaultValue("15m") Duration accessTokenTtl) {
}
