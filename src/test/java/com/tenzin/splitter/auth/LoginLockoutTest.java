package com.tenzin.splitter.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.MutableClock;
import com.tenzin.splitter.user.User;
import com.tenzin.splitter.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

/** Phase 2 "done when": the 6th attempt is refused even with the right password. */
@IntegrationTest
class LoginLockoutTest extends ApiTestSupport {

	private static final String WRONG = "wrong-password";

	@Autowired
	private MutableClock clock;

	@Autowired
	private UserRepository users;

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

	private void failLogins(int times) throws Exception {
		for (int i = 0; i < times; i++) {
			login(email, WRONG).andExpect(status().isUnauthorized());
		}
	}

	@Test
	void sixthAttemptIsRefusedEvenWithTheRightPassword() throws Exception {
		failLogins(5);

		login(email, PASSWORD)
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.detail").value(AuthService.INVALID_CREDENTIALS));
	}

	@Test
	void fourFailuresDoNotLock() throws Exception {
		failLogins(4);

		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void lockLiftsAfterFifteenMinutes() throws Exception {
		failLogins(5);

		clock.advance(Duration.ofMinutes(14));
		login(email, PASSWORD).andExpect(status().isUnauthorized());

		clock.advance(Duration.ofMinutes(1).plusSeconds(1));
		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void successfulLoginResetsTheCounter() throws Exception {
		failLogins(4);
		login(email, PASSWORD).andExpect(status().isOk());
		failLogins(4);

		login(email, PASSWORD).andExpect(status().isOk());
		assertThat(users.findByEmail(email).orElseThrow().getFailedAttempts()).isZero();
	}

	@Test
	void attemptsDuringLockoutDoNotExtendIt() throws Exception {
		failLogins(5);
		clock.advance(Duration.ofMinutes(10));
		failLogins(3);

		clock.advance(Duration.ofMinutes(5).plusSeconds(1));
		login(email, PASSWORD).andExpect(status().isOk());
	}

	@Test
	void parallelGuessesCannotGetPastTheLimit() throws Exception {
		int threads = 20;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(pool.submit(() -> {
				start.await();
				login(email, WRONG);
				return null;
			}));
		}
		start.countDown();
		for (Future<?> result : results) {
			result.get();
		}
		pool.shutdown();

		User user = users.findByEmail(email).orElseThrow();
		assertThat(user.getLockedUntil()).isAfter(clock.instant());
		login(email, PASSWORD).andExpect(status().isUnauthorized());
	}

}
