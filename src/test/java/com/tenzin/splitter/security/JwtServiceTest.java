package com.tenzin.splitter.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	static final String SECRET = "dGVzdC1vbmx5LWp3dC1zZWNyZXQtMzItYnl0ZXMtISE=";

	static final String OTHER_SECRET = Base64.getEncoder()
		.encodeToString("a-completely-different-32b-key!!".getBytes(StandardCharsets.UTF_8));

	static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

	private static JwtService service(String secret, Instant now) {
		return new JwtService(new JwtProperties(secret, Duration.ofMinutes(15)), Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void roundTripsUserIdAndEmail() {
		JwtService jwt = service(SECRET, NOW);
		AuthUser user = jwt.verify(jwt.createAccessToken(42, "a@example.com"));
		assertThat(user).isEqualTo(new AuthUser(42, "a@example.com"));
	}

	@Test
	void reportsFifteenMinuteLifetime() {
		assertThat(service(SECRET, NOW).accessTokenTtlSeconds()).isEqualTo(900);
	}

	@Test
	void validJustBeforeExpiryAndRejectedAfter() {
		String token = service(SECRET, NOW).createAccessToken(42, "a@example.com");

		assertThat(service(SECRET, NOW.plus(Duration.ofMinutes(14))).verify(token).id()).isEqualTo(42);
		assertThatThrownBy(() -> service(SECRET, NOW.plus(Duration.ofMinutes(16))).verify(token))
			.isInstanceOf(ExpiredJwtException.class);
	}

	@Test
	void rejectsForgedPayload() {
		JwtService jwt = service(SECRET, NOW);
		String forged = JwtTestTokens.withForgedSubject(jwt.createAccessToken(42, "a@example.com"), 999);
		assertThatThrownBy(() -> jwt.verify(forged)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsTokenSignedWithAnotherKey() {
		String token = service(OTHER_SECRET, NOW).createAccessToken(42, "a@example.com");
		assertThatThrownBy(() -> service(SECRET, NOW).verify(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void rejectsGarbage() {
		assertThatThrownBy(() -> service(SECRET, NOW).verify("not.a.jwt")).isInstanceOf(JwtException.class);
	}

	@Test
	void refusesToStartWithoutASecret() {
		assertThatThrownBy(() -> service("", NOW)).hasMessageContaining("JWT_SECRET is not set");
	}

	@Test
	void refusesKeysShorterThan256Bits() {
		String shortKey = Base64.getEncoder().encodeToString(new byte[16]);
		assertThatThrownBy(() -> service(shortKey, NOW)).hasMessageContaining("at least 256 bits");
	}

}
