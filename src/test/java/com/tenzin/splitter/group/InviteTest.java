package com.tenzin.splitter.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.MutableClock;
import com.tenzin.splitter.common.TokenGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

/** Phase 3 "done when": an invite works once, then fails. */
@IntegrationTest
class InviteTest extends ApiTestSupport {

	@Autowired
	private MutableClock clock;

	@Autowired
	private GroupInviteRepository invites;

	@Autowired
	private GroupMemberRepository members;

	private String ownerToken;

	private long groupId;

	@BeforeEach
	void setUpGroup() throws Exception {
		ownerToken = registerAndLogin(uniqueEmail());
		groupId = createGroup(ownerToken, "Goa trip");
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private String accept(String userToken, String invite, int expectedStatus) throws Exception {
		return postAs(userToken, "/api/invites/" + invite + "/accept")
			.andExpect(status().is(expectedStatus))
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	@Test
	void createReturnsTokenOnceAndStoresOnlyItsHash() throws Exception {
		postAs(ownerToken, "/api/groups/" + groupId + "/invites")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.token").isString())
			.andExpect(jsonPath("$.acceptPath").isString())
			.andExpect(jsonPath("$.expiresAt").isString());

		String token = createInvite(ownerToken, groupId);
		assertThat(invites.findByTokenHash(TokenGenerator.sha256(token))).isPresent();
		assertThat(invites.findByTokenHash(token)).isEmpty();
	}

	@Test
	void inviteWorksOnceThenFails() throws Exception {
		String invite = createInvite(ownerToken, groupId);
		String first = registerAndLogin(uniqueEmail());
		String second = registerAndLogin(uniqueEmail());

		postAs(first, "/api/invites/" + invite + "/accept")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(groupId))
			.andExpect(jsonPath("$.members", hasSize(2)));

		postAs(second, "/api/invites/" + invite + "/accept")
			.andExpect(status().isGone())
			.andExpect(jsonPath("$.detail").value(InviteService.INVITE_GONE));
		getAs(second, "/api/groups/" + groupId).andExpect(status().isForbidden());
	}

	@Test
	void expiredInviteIsGone() throws Exception {
		String invite = createInvite(ownerToken, groupId);

		clock.advance(Duration.ofHours(48).plusSeconds(1));

		accept(registerAndLogin(uniqueEmail()), invite, 410);
	}

	@Test
	void inviteStillWorksJustBeforeExpiry() throws Exception {
		String invite = createInvite(ownerToken, groupId);

		clock.advance(Duration.ofHours(47));

		accept(registerAndLogin(uniqueEmail()), invite, 200);
	}

	@Test
	void unknownInviteIsNotFound() throws Exception {
		accept(registerAndLogin(uniqueEmail()), "no-such-invite", 404);
	}

	@Test
	void existingMemberIsTurnedAwayWithoutUsingUpTheInvite() throws Exception {
		String invite = createInvite(ownerToken, groupId);

		accept(ownerToken, invite, 409);

		accept(registerAndLogin(uniqueEmail()), invite, 200);
	}

	@Test
	void anonymousCannotAccept() throws Exception {
		String invite = createInvite(ownerToken, groupId);
		postJson("/api/invites/" + invite + "/accept", "").andExpect(status().isUnauthorized());
	}

	/** Two people click the same link at the same moment: exactly one of them joins. */
	@Test
	void concurrentAcceptsLetExactlyOnePersonJoin() throws Exception {
		String invite = createInvite(ownerToken, groupId);
		int racers = 8;
		List<String> tokens = new ArrayList<>();
		for (int i = 0; i < racers; i++) {
			tokens.add(registerAndLogin(uniqueEmail()));
		}

		ExecutorService pool = Executors.newFixedThreadPool(racers);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		for (String token : tokens) {
			Callable<Integer> attempt = () -> {
				start.await();
				return postAs(token, "/api/invites/" + invite + "/accept").andReturn().getResponse().getStatus();
			};
			results.add(pool.submit(attempt));
		}
		start.countDown();
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		pool.shutdown();

		assertThat(statuses).filteredOn(s -> s == 200).hasSize(1);
		assertThat(statuses).filteredOn(s -> s == 410).hasSize(racers - 1);
		assertThat(members.findMembersWithUsers(groupId)).hasSize(2);
	}

}
