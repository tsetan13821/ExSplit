package com.tenzin.splitter.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class JwtTestTokens {

	private JwtTestTokens() {
	}

	/**
	 * Swaps the payload for one claiming a different user, keeping the original header and
	 * signature: what an attacker editing a token by hand would send.
	 */
	public static String withForgedSubject(String token, long subject) {
		String[] parts = token.split("\\.");
		String payload = """
				{"sub":"%d","email":"attacker@example.com","iat":1767261600,"exp":4102444800}"""
			.formatted(subject);
		String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return parts[0] + "." + encoded + "." + parts[2];
	}

}
