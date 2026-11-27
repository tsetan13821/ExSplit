package com.tenzin.splitter.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

/**
 * Opaque tokens for refresh, invite and password-reset links.
 *
 * The raw token goes to the client once; only its SHA-256 hash is stored. A fast hash is
 * fine here (unlike passwords) because 256 random bits cannot be brute-forced, and the
 * server must be able to look the token up by its hash.
 */
@Component
public class TokenGenerator {

	private static final int TOKEN_BYTES = 32;

	private final SecureRandom random = new SecureRandom();

	/** 32 random bytes, Base64URL without padding (43 characters, safe in URLs). */
	public String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Lowercase hex SHA-256, 64 characters. */
	public static String sha256(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is always available on the JVM", ex);
		}
	}

}
