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

	/** Same message for unknown email and wrong password, so callers cannot probe which emails exist. */
	static final String INVALID_CREDENTIALS = "Invalid email or password";

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	private final JwtService jwtService;

	private final Clock clock;

	/**
	 * Checked against when the email is unknown, so that path costs one BCrypt
	 * comparison like a real login and response time does not reveal the answer.
	 */
	private final String dummyHash;

	public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService, Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
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

	@Transactional(readOnly = true)
	public TokenResponse login(LoginRequest request) {
		User user = users.findByEmail(normaliseEmail(request.email())).orElse(null);
		if (user == null) {
			passwordEncoder.matches(request.password(), dummyHash);
			throw ApiException.unauthorized(INVALID_CREDENTIALS);
		}
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw ApiException.unauthorized(INVALID_CREDENTIALS);
		}
		String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail());
		return TokenResponse.bearer(accessToken, jwtService.accessTokenTtlSeconds());
	}

	static String normaliseEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
