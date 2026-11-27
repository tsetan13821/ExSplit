package com.tenzin.splitter.auth;

/**
 * @param expiresIn access token lifetime in seconds
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn) {

	public static TokenResponse bearer(String accessToken, long expiresIn) {
		return new TokenResponse(accessToken, "Bearer", expiresIn);
	}

}
