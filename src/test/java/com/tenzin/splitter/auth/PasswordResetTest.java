package com.tenzin.splitter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.CapturingMailSender;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.MutableClock;
import com.tenzin.splitter.common.TokenGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class PasswordResetTest extends ApiTestSupport {

	private static final Pattern TOKEN_LINE = Pattern.compile("^([A-Za-z0-9_-]{43})$", Pattern.MULTILINE);

	private static final String NEW_PASSWORD = "a-brand-new-password";

	private static final Duration MAIL_WAIT = Duration.ofSeconds(5);

	@Autowired
	private CapturingMailSender mail;

	@Autowired
	private MutableClock clock;

	@Autowired
	private PasswordResetTokenRepository resetTokens;

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

	private ResultActions requestReset(String address) throws Exception {
		return postJson("/api/auth/password-reset/request", """
				{"email": "%s"}
				""".formatted(address));
	}

	private ResultActions confirm(String token, String newPassword) throws Exception {
		return postJson("/api/auth/password-reset/confirm", """
				{"token": "%s", "newPassword": "%s"}
				""".formatted(token, newPassword));
	}

	/** Requests a reset and reads the token out of the email, as the user would. */
	private String tokenFromEmail() throws Exception {
		int before = mail.messagesTo(email).size();
		requestReset(email).andExpect(status().isAccepted());
		long deadline = System.nanoTime() + MAIL_WAIT.toNanos();
		while (mail.messagesTo(email).size() == before && System.nanoTime() < deadline) {
			Thread.sleep(20);
		}
		SimpleMailMessage latest = mail.messagesTo(email).getLast();
		Matcher matcher = TOKEN_LINE.matcher(latest.getText());
		assertThat(matcher.find()).as("reset token in email body").isTrue();
		return matcher.group(1);
	}

	@Test
	void requestSendsAnEmailWithASingleUseToken() throws Exception {
		requestReset(email.toUpperCase()).andExpect(status().isAccepted());

		SimpleMailMessage message = mail.awaitMessageTo(email, MAIL_WAIT);
		assertThat(message.getSubject()).contains("Reset");
		assertThat(message.getText()).contains("Asha").contains("30 minutes").contains("/api/auth/password-reset/confirm");

		Matcher matcher = TOKEN_LINE.matcher(message.getText());
		assertThat(matcher.find()).isTrue();
		assertThat(resetTokens.findByTokenHash(TokenGenerator.sha256(matcher.group(1)))).isPresent();
	}

	@Test
	void unknownEmailGetsTheSame202AndNoEmail() throws Exception {
		String unknown = uniqueEmail();

		requestReset(unknown).andExpect(status().isAccepted());

		// Mail is async: wait for a later email to a real account, then check none went to the unknown one.
		tokenFromEmail();
		assertThat(mail.messagesTo(unknown)).isEmpty();
	}

	@Test
	void confirmSetsTheNewPassword() throws Exception {
		confirm(tokenFromEmail(), NEW_PASSWORD).andExpect(status().isNoContent());

		login(email, NEW_PASSWORD).andExpect(status().isOk());
		login(email, PASSWORD).andExpect(status().isUnauthorized());
	}

	@Test
	void resetRevokesEveryRefreshToken() throws Exception {
		String laptop = json(login(email, PASSWORD).andExpect(status().isOk()), "$.refreshToken");
		String phone = json(login(email, PASSWORD).andExpect(status().isOk()), "$.refreshToken");

		confirm(tokenFromEmail(), NEW_PASSWORD).andExpect(status().isNoContent());

		refresh(laptop).andExpect(status().isUnauthorized());
		refresh(phone).andExpect(status().isUnauthorized());
	}

	@Test
	void tokenWorksOnlyOnce() throws Exception {
		String token = tokenFromEmail();
		confirm(token, NEW_PASSWORD).andExpect(status().isNoContent());

		confirm(token, "yet-another-password")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value(PasswordResetService.INVALID_RESET_TOKEN));
		login(email, NEW_PASSWORD).andExpect(status().isOk());
	}

	@Test
	void usingOneLinkKillsOtherOutstandingLinks() throws Exception {
		String first = tokenFromEmail();
		String second = tokenFromEmail();

		confirm(second, NEW_PASSWORD).andExpect(status().isNoContent());

		confirm(first, "yet-another-password").andExpect(status().isBadRequest());
	}

	@Test
	void tokenExpiresAfterThirtyMinutes() throws Exception {
		String token = tokenFromEmail();

		clock.advance(Duration.ofMinutes(30).plusSeconds(1));

		confirm(token, NEW_PASSWORD).andExpect(status().isBadRequest());
		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void unknownTokenIsRejected() throws Exception {
		confirm("not-a-real-token", NEW_PASSWORD).andExpect(status().isBadRequest());
	}

	@Test
	void weakNewPasswordFailsValidation() throws Exception {
		confirm(tokenFromEmail(), "short")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("newPassword"));
	}

	@Test
	void resetUnlocksALockedAccount() throws Exception {
		for (int i = 0; i < 5; i++) {
			login(email, "wrong-password").andExpect(status().isUnauthorized());
		}
		login(email, PASSWORD).andExpect(status().isUnauthorized());

		confirm(tokenFromEmail(), NEW_PASSWORD).andExpect(status().isNoContent());

		login(email, NEW_PASSWORD).andExpect(status().isOk());
	}

}
