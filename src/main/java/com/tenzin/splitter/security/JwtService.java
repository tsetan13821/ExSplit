package com.tenzin.splitter.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

/**
 * Issues and verifies short-lived HS256 access tokens.
 * Claims: sub = user id, email, iat, exp. No roles or group ids, so nothing in the
 * token can go stale when memberships change.
 */
@Service
public class JwtService {

	private static final String EMAIL_CLAIM = "email";

	private static final int MIN_KEY_BYTES = 32; // 256 bits, required for HS256

	private final SecretKey key;

	private final JwtProperties properties;

	private final Clock clock;

	private final JwtParser parser;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.key = buildKey(properties.secret());
		this.parser = Jwts.parser()
			.verifyWith(key)
			.clock(() -> Date.from(clock.instant()))
			.build();
	}

	public String createAccessToken(long userId, String email) {
		Instant now = clock.instant();
		return Jwts.builder()
			.subject(Long.toString(userId))
			.claim(EMAIL_CLAIM, email)
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(properties.accessTokenTtl())))
			.signWith(key, Jwts.SIG.HS256)
			.compact();
	}

	/**
	 * Verifies signature and expiry.
	 * @throws JwtException if the token is malformed, forged, signed with another key or expired
	 */
	public AuthUser verify(String token) {
		Claims claims = parser.parseSignedClaims(token).getPayload();
		String subject = claims.getSubject();
		String email = claims.get(EMAIL_CLAIM, String.class);
		if (subject == null || email == null) {
			throw new JwtException("Token is missing required claims");
		}
		try {
			return new AuthUser(Long.parseLong(subject), email);
		}
		catch (NumberFormatException ex) {
			throw new JwtException("Token subject is not a user id");
		}
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

	private static SecretKey buildKey(String secret) {
		if (secret == null || secret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET is not set");
		}
		byte[] bytes;
		try {
			bytes = Decoders.BASE64.decode(secret.trim());
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException("JWT_SECRET must be Base64-encoded", ex);
		}
		if (bytes.length < MIN_KEY_BYTES) {
			throw new IllegalStateException("JWT_SECRET must decode to at least 256 bits");
		}
		return Keys.hmacShaKeyFor(bytes);
	}

}
