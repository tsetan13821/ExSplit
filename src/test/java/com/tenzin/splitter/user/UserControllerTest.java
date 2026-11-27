package com.tenzin.splitter.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.security.JwtProperties;
import com.tenzin.splitter.security.JwtService;
import com.tenzin.splitter.security.JwtTestTokens;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

/** Phase 1 "done when": /users/me is 401 for every bad token and 200 for a valid one. */
@IntegrationTest
class UserControllerTest extends ApiTestSupport {

	private static final String ME = "/api/users/me";

	@Autowired
	private JwtProperties jwtProperties;

	@Autowired
	private UserRepository users;

	@Test
	void validTokenReturnsCurrentUser() throws Exception {
		String email = uniqueEmail();
		String token = registerAndLogin(email);

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(token)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.name").value("Test User"));
	}

	@Test
	void noTokenIsUnauthorized() throws Exception {
		mvc.perform(get(ME))
			.andExpect(status().isUnauthorized())
			.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(401))
			.andExpect(jsonPath("$.detail").value("Authentication required"));
	}

	@Test
	void malformedTokenIsUnauthorized() throws Exception {
		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid access token"));
	}

	@Test
	void tamperedTokenIsUnauthorized() throws Exception {
		String token = registerAndLogin(uniqueEmail());
		long otherUserId = users.findByEmail(registerOther()).orElseThrow().getId();

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION,
				bearer(JwtTestTokens.withForgedSubject(token, otherUserId))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Invalid access token"));
	}

	@Test
	void expiredTokenIsUnauthorized() throws Exception {
		String email = uniqueEmail();
		registerAndLogin(email);
		long userId = users.findByEmail(email).orElseThrow().getId();
		Clock anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
		String expired = new JwtService(jwtProperties, anHourAgo).createAccessToken(userId, email);

		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value("Access token expired"));
	}

	@Test
	void nonBearerSchemeIsUnauthorized() throws Exception {
		mvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
			.andExpect(status().isUnauthorized());
	}

	private String registerOther() throws Exception {
		String email = uniqueEmail();
		register(email, "Victim", PASSWORD).andExpect(status().isCreated());
		return email;
	}

}
