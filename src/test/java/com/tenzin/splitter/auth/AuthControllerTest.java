package com.tenzin.splitter.auth;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

@IntegrationTest
class AuthControllerTest extends ApiTestSupport {

	@Test
	void registerCreatesUserWithLowercasedEmailAndNoPasswordInResponse() throws Exception {
		String email = uniqueEmail();

		register(email.toUpperCase(), "Asha", PASSWORD)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.name").value("Asha"))
			.andExpect(jsonPath("$.passwordHash").doesNotExist())
			.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	void registerWithExistingEmailIsConflictRegardlessOfCase() throws Exception {
		String email = uniqueEmail();
		register(email, "First", PASSWORD).andExpect(status().isCreated());

		register(email.toUpperCase(), "Second", PASSWORD)
			.andExpect(status().isConflict())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.detail").value("Email is already registered"));
	}

	@Test
	void registerRejectsInvalidInputWithFieldErrors() throws Exception {
		register("not-an-email", "", "short")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("email", "name", "password")));
	}

	@Test
	void loginReturnsBearerAccessToken() throws Exception {
		String email = uniqueEmail();
		register(email, "Asha", PASSWORD).andExpect(status().isCreated());

		login(email.toUpperCase(), PASSWORD)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken", not(emptyString())))
			.andExpect(jsonPath("$.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.expiresIn").value(900));
	}

	@Test
	void loginWithWrongPasswordIsUnauthorized() throws Exception {
		String email = uniqueEmail();
		register(email, "Asha", PASSWORD).andExpect(status().isCreated());

		login(email, "wrong-password")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(AuthService.INVALID_CREDENTIALS));
	}

	@Test
	void loginWithUnknownEmailGivesTheSameMessageAsWrongPassword() throws Exception {
		login(uniqueEmail(), PASSWORD)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(AuthService.INVALID_CREDENTIALS));
	}

}
