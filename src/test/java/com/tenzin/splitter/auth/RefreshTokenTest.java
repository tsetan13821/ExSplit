package com.tenzin.splitter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.MutableClock;
import com.tenzin.splitter.common.TokenGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

/** Phase 2 "done when": a rotated token is rejected and reusing it revokes the whole family. */
@IntegrationTest
class RefreshTokenTest extends ApiTestSupport {

	@Autowired
	private MutableClock clock;

	@Autowired
	private RefreshTokenRepository refreshTokens;

	private String email;

	@BeforeEach
	void createUser() throws Exception {
		email = uniqueEmail();
		register(email, "Asha", PASSWORD).andExpect(status().isCreated());
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private String loginRefreshToken() throws Exception {
		return json(login(email, PASSWORD).andExpect(status().isOk()), "$.refreshToken");
	}

	@Test
	void loginReturnsRefreshTokenAndOnlyItsHashIsStored() throws Exception {
		String token = loginRefreshToken();

		assertThat(token).hasSize(43);
		assertThat(refreshTokens.findByTokenHash(TokenGenerator.sha256(token))).isPresent();
		assertThat(refreshTokens.findByTokenHash(token)).isEmpty();
	}

	@Test
	void refreshReturnsNewWorkingPair() throws Exception {
		String original = loginRefreshToken();

		ResultActions result = refresh(original)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken", not(emptyString())))
			.andExpect(jsonPath("$.refreshToken", not(original)));

		String accessToken = json(result, "$.accessToken");
		mvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));
	}

	@Test
	void rotatedTokenIsRejected() throws Exception {
		String original = loginRefreshToken();
		refresh(original).andExpect(status().isOk());

		refresh(original)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(RefreshTokenService.INVALID_REFRESH_TOKEN));
	}

	@Test
	void reusingRotatedTokenRevokesTheWholeFamily() throws Exception {
		String stolen = loginRefreshToken();
		String legitimate = json(refresh(stolen).andExpect(status().isOk()), "$.refreshToken");

		refresh(stolen).andExpect(status().isUnauthorized()); // attacker replays the old token

		refresh(legitimate).andExpect(status().isUnauthorized()); // so the newest one is dead too
	}

	@Test
	void reuseDoesNotAffectOtherSessions() throws Exception {
		String laptop = loginRefreshToken();
		String phone = loginRefreshToken();
		refresh(laptop).andExpect(status().isOk());

		refresh(laptop).andExpect(status().isUnauthorized());

		refresh(phone).andExpect(status().isOk());
	}

	@Test
	void logoutRevokesTheFamily() throws Exception {
		String original = loginRefreshToken();
		String rotated = json(refresh(original).andExpect(status().isOk()), "$.refreshToken");

		logout(rotated).andExpect(status().isNoContent());

		refresh(rotated).andExpect(status().isUnauthorized());
	}

	@Test
	void logoutIsIdempotent() throws Exception {
		String token = loginRefreshToken();
		logout(token).andExpect(status().isNoContent());
		logout(token).andExpect(status().isNoContent());
		logout("never-issued").andExpect(status().isNoContent());
	}

	@Test
	void expiredRefreshTokenIsRejected() throws Exception {
		String token = loginRefreshToken();

		clock.advance(Duration.ofDays(7).plusSeconds(1));

		refresh(token).andExpect(status().isUnauthorized());
	}

	@Test
	void refreshTokenStillWorksJustBeforeExpiry() throws Exception {
		String token = loginRefreshToken();

		clock.advance(Duration.ofDays(7).minusMinutes(1));

		refresh(token).andExpect(status().isOk());
	}

	@Test
	void unknownTokenIsRejected() throws Exception {
		refresh("never-issued").andExpect(status().isUnauthorized());
	}

	@Test
	void blankTokenIsBadRequest() throws Exception {
		refresh("").andExpect(status().isBadRequest());
	}

}
