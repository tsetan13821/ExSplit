package com.tenzin.splitter.auth;

import java.time.Clock;
import java.time.Instant;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.common.TokenGenerator;
import com.tenzin.splitter.user.User;
import com.tenzin.splitter.user.UserRepository;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Password reset by email link. Same token recipe as refresh tokens and invites:
 * random token in the email, SHA-256 hash in the table, 30-minute expiry, single use.
 */
@Service
public class PasswordResetService {

	static final String INVALID_RESET_TOKEN = "Reset link is invalid or has expired";

	private final UserRepository users;

	private final PasswordResetTokenRepository resetTokens;

	private final RefreshTokenRepository refreshTokens;

	private final TokenGenerator tokenGenerator;

	private final PasswordEncoder passwordEncoder;

	private final ApplicationEventPublisher events;

	private final AuthProperties properties;

	private final Clock clock;

	public PasswordResetService(UserRepository users, PasswordResetTokenRepository resetTokens,
			RefreshTokenRepository refreshTokens, TokenGenerator tokenGenerator, PasswordEncoder passwordEncoder,
			ApplicationEventPublisher events, AuthProperties properties, Clock clock) {
		this.users = users;
		this.resetTokens = resetTokens;
		this.refreshTokens = refreshTokens;
		this.tokenGenerator = tokenGenerator;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
		this.properties = properties;
		this.clock = clock;
	}

	/**
	 * Does nothing visible for an unknown email: the endpoint answers 202 either way, and
	 * the email itself is sent asynchronously after commit, so neither the response nor
	 * its timing reveals whether the address has an account.
	 */
	@Transactional
	public void requestReset(String email) {
		users.findByEmail(AuthService.normaliseEmail(email)).ifPresent(user -> {
			String token = tokenGenerator.newToken();
			Instant now = clock.instant();
			resetTokens.save(new PasswordResetToken(user.getId(), TokenGenerator.sha256(token),
					now.plus(properties.passwordResetTtl()), now));
			events.publishEvent(new PasswordResetRequested(user.getEmail(), user.getName(), token,
					properties.passwordResetTtl()));
		});
	}

	/**
	 * Sets the new password, then signs the user out everywhere by revoking every refresh
	 * token: whoever might have known the old password loses their session too. Access
	 * tokens already issued still expire on their own within 15 minutes. Also clears any
	 * login lockout, since the user has just proved they control the email address.
	 */
	@Transactional
	public void confirmReset(String token, String newPassword) {
		PasswordResetToken reset = resetTokens.findByTokenHash(TokenGenerator.sha256(token))
			.orElseThrow(() -> ApiException.badRequest(INVALID_RESET_TOKEN));
		Instant now = clock.instant();
		if (resetTokens.claim(reset.getId(), now) == 0) {
			throw ApiException.badRequest(INVALID_RESET_TOKEN);
		}

		User user = users.findById(reset.getUserId()).orElseThrow(() -> ApiException.badRequest(INVALID_RESET_TOKEN));
		user.setPasswordHash(passwordEncoder.encode(newPassword));
		user.setFailedAttempts(0);
		user.setLockedUntil(null);

		resetTokens.invalidateAllForUser(user.getId(), now);
		refreshTokens.revokeAllForUser(user.getId(), now);
	}

}
