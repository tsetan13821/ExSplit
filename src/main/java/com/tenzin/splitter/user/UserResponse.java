package com.tenzin.splitter.user;

import java.time.Instant;

/** Public view of a user. Never exposes the password hash or lockout state. */
public record UserResponse(Long id, String email, String name, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getName(), user.getCreatedAt());
	}

}
