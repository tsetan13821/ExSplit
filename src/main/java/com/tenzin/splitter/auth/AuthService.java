package com.tenzin.splitter.auth;

import java.time.Clock;
import java.util.Locale;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.security.JwtService;
import com.tenzin.splitter.user.User;
import com.tenzin.splitter.user.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

	/**
	 * One message for unknown email, wrong password and locked account, so callers cannot
	 * probe which emails exist (unknown emails never lock, so a distinct "locked" message
	 * would give them away).
	 */
	static final String INVALID_CREDENTIALS = "Invalid email or password";

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final RefreshTokenService refreshTokens;

	private final LoginAttemptService loginAttempts;

	private final Clock clock;

	/**
	 * Checked against when no real hash is, so every failed login costs one BCrypt
	 * comparison and response time does not reveal why it failed.
	 */
	private final String dummyHash;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService,
			RefreshTokenService refreshTokens, LoginAttemptService loginAttempts, Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
		this.refreshTokens = refreshTokens;
		this.loginAttempts = loginAttempts;
		this.clock = clock;
		this.dummyHash = passwordEncoder.encode("timing-equaliser-not-a-real-password");
	}

	@Transactional
	public User register(RegisterRequest request) {
		String email = normaliseEmail(request.email());
		if (users.existsByEmail(email)) {
			throw ApiException.conflict("Email is already registered");
		}
		User user = new User(email, request.name().trim(), passwordEncoder.encode(request.password()),
				clock.instant());
		try {
			return users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Two registrations raced past the exists check; the unique constraint caught it.
			throw ApiException.conflict("Email is already registered");
		}
	}

	/**
	 * The lock is checked before the password, so a locked account is refused even with
	 * the right password. Commits on failure too, otherwise the failed attempt would be
	 * rolled back along with the request.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse login(LoginRequest request) {
		User user = users.findByEmail(normaliseEmail(request.email())).orElse(null);
		if (user == null || loginAttempts.isLocked(user)) {
			passwordEncoder.matches(request.password(), dummyHash);
			throw ApiException.unauthorized(INVALID_CREDENTIALS);
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			loginAttempts.recordFailure(user);
			throw ApiException.unauthorized(INVALID_CREDENTIALS);
		}
		loginAttempts.recordSuccess(user);
		return issueTokens(user, refreshTokens.issueNewFamily(user));
	}

	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse refresh(String refreshToken) {
		RefreshTokenService.Rotation rotation = refreshTokens.rotate(refreshToken);
		return issueTokens(rotation.user(), rotation.refreshToken());
	}

	@Transactional
	public void logout(String refreshToken) {
		refreshTokens.revokeFamilyOf(refreshToken);
	}

	private TokenResponse issueTokens(User user, String refreshToken) {
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return TokenResponse.bearer(accessToken, refreshToken, jwtService.accessTokenTtlSeconds());
	}

	static String normaliseEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
