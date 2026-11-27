package com.tenzin.splitter.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import com.tenzin.splitter.common.ApiException;
import com.tenzin.splitter.common.TokenGenerator;
import com.tenzin.splitter.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are the stateful half of authentication: access tokens cannot be
 * revoked, but these can, and they are rotated on every use.
 *
 * Methods that reject a token still commit, because detecting reuse must leave the
 * family revoked even though the request fails. Hence {@code noRollbackFor}.
 */
@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

	static final String INVALID_REFRESH_TOKEN = "Invalid refresh token";

	private final RefreshTokenRepository refreshTokens;

	private final TokenGenerator tokenGenerator;

	private final AuthProperties properties;

	private final Clock clock;

	public RefreshTokenService(RefreshTokenRepository refreshTokens, TokenGenerator tokenGenerator,
			AuthProperties properties, Clock clock) {
		this.refreshTokens = refreshTokens;
		this.tokenGenerator = tokenGenerator;
		this.properties = properties;
		this.clock = clock;
	}

	/** Starts a new family, one per login. Returns the raw token for the client. */
	@Transactional
	public String issueNewFamily(User user) {
		return issue(user, UUID.randomUUID());
	}

	/**
	 * Exchanges a refresh token for a new one in the same family and revokes the old one.
	 * Presenting an already-revoked token means it was copied: the whole family is revoked
	 * and the caller has to log in again.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public Rotation rotate(String rawToken) {
		RefreshToken current = refreshTokens.findByTokenHash(TokenGenerator.sha256(rawToken))
			.orElseThrow(() -> ApiException.unauthorized(INVALID_REFRESH_TOKEN));
		Instant now = clock.instant();

		if (current.isRevoked()) {
			throw revokeFamilyAfterReuse(current, now);
		}
		if (current.isExpiredAt(now)) {
			throw ApiException.unauthorized(INVALID_REFRESH_TOKEN);
		}
		if (refreshTokens.revokeIfActive(current.getId(), now) == 0) {
			// A concurrent request rotated this token between our read and our update.
			throw revokeFamilyAfterReuse(current, now);
		}

		User user = current.getUser();
		return new Rotation(user, issue(user, current.getFamilyId()));
	}

	/** Logout. Idempotent: unknown or already-revoked tokens are ignored. */
	@Transactional
	public void revokeFamilyOf(String rawToken) {
		refreshTokens.findByTokenHash(TokenGenerator.sha256(rawToken))
			.ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), clock.instant()));
	}

	private ApiException revokeFamilyAfterReuse(RefreshToken token, Instant now) {
		int revoked = refreshTokens.revokeFamily(token.getFamilyId(), now);
		log.warn("Refresh token reuse detected for user {}; revoked {} token(s) in family {}",
				token.getUser().getId(), revoked, token.getFamilyId());
		return ApiException.unauthorized(INVALID_REFRESH_TOKEN);
	}

	private String issue(User user, UUID familyId) {
		String raw = tokenGenerator.newToken();
		Instant now = clock.instant();
		refreshTokens.save(new RefreshToken(user, TokenGenerator.sha256(raw), familyId,
				now.plus(properties.refreshTokenTtl()), now));
		return raw;
	}

	public record Rotation(User user, String refreshToken) {
	}

}
