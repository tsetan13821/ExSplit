package com.tenzin.splitter.auth;

import java.time.Clock;
import java.time.Instant;

import com.tenzin.splitter.user.User;
import com.tenzin.splitter.user.UserRepository;

import org.springframework.stereotype.Service;

/**
 * Locks an account for {@code lockoutDuration} after {@code maxFailedAttempts} wrong
 * passwords in a row. Runs inside the caller's login transaction.
 */
@Service
public class LoginAttemptService {

	private final UserRepository users;

	private final AuthProperties properties;

	private final Clock clock;

	public LoginAttemptService(UserRepository users, AuthProperties properties, Clock clock) {
		this.users = users;
		this.properties = properties;
		this.clock = clock;
	}

	public boolean isLocked(User user) {
		Instant lockedUntil = user.getLockedUntil();
		return lockedUntil != null && lockedUntil.isAfter(clock.instant());
	}

	/**
	 * Counted with one atomic UPDATE rather than read-modify-write, so parallel guesses
	 * cannot overwrite each other's increments and sneak past the limit.
	 */
	public void recordFailure(User user) {
		Instant lockUntil = clock.instant().plus(properties.lockoutDuration());
		users.recordFailedLogin(user.getId(), properties.maxFailedAttempts(), lockUntil);
	}

	public void recordSuccess(User user) {
		if (user.getFailedAttempts() != 0 || user.getLockedUntil() != null) {
			user.setFailedAttempts(0);
			user.setLockedUntil(null);
		}
	}

}
