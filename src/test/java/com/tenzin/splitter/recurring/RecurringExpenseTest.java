package com.tenzin.splitter.recurring;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import com.tenzin.splitter.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/** Phase 6 "done when": with a controlled clock, the job posts exactly once. */
@IntegrationTest
class RecurringExpenseTest extends ApiTestSupport {

	private static final Instant MARCH_10 = Instant.parse("2026-03-10T10:00:00Z");

	@Autowired
	private MutableClock clock;

	@Autowired
	private RecurringExpenseJob job;

	private String owner;

	private String member;

	private String memberEmail;

	private long ownerId;

	private long memberId;

	private long groupId;

	@BeforeEach
	void setUp() throws Exception {
		clock.travelTo(MARCH_10);
		owner = registerAndLogin(uniqueEmail());
		memberEmail = uniqueEmail();
		member = registerAndLogin(memberEmail);
		ownerId = userId(owner);
		memberId = userId(member);
		groupId = createGroup(owner, "Flat 4B");
		addMember(owner, groupId, member);
	}

	@AfterEach
	void resetClock() {
		clock.reset();
	}

	private String url() {
		return "/api/groups/" + groupId + "/recurring-expenses";
	}

	private ResultActions createRent(String token, int dayOfMonth) throws Exception {
		return postAs(token, url(), """
				{"description": "Rent", "amountPaise": 3000001, "paidBy": %d, "dayOfMonth": %d}
				""".formatted(ownerId, dayOfMonth));
	}

	/**
	 * Logs in again before reading: after the clock jumps a month, the member's original
	 * 15-minute access token has (correctly) expired.
	 */
	private String freshMemberToken() throws Exception {
		return json(login(memberEmail, PASSWORD).andExpect(status().isOk()), "$.accessToken");
	}

	private ResultActions expenses() throws Exception {
		return getAs(freshMemberToken(), "/api/groups/" + groupId + "/expenses").andExpect(status().isOk());
	}

	private ResultActions recurringList() throws Exception {
		return getAs(freshMemberToken(), url()).andExpect(status().isOk());
	}

	@Test
	void dueTodayIsPostedOnceAndMovesToNextMonth() throws Exception {
		createRent(member, 10)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.nextRunDate").value("2026-03-10"))
			.andExpect(jsonPath("$.createdBy").value(memberId));

		job.run();

		expenses().andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].description").value("Rent"))
			.andExpect(jsonPath("$.content[0].expenseDate").value("2026-03-10"))
			.andExpect(jsonPath("$.content[0].paidBy").value(ownerId))
			.andExpect(jsonPath("$.content[0].splitType").value("EQUAL"))
			// ₹30,000.01 split two ways: the extra paisa goes to the lower user id
			.andExpect(jsonPath("$.content[0].shares[*].sharePaise", contains(1500001, 1500000)));
		recurringList().andExpect(jsonPath("$[0].nextRunDate").value("2026-04-10"));
	}

	@Test
	void runningAgainTheSameDayPostsNothing() throws Exception {
		createRent(owner, 10).andExpect(status().isCreated());

		job.run();
		job.run();

		expenses().andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void nextMonthPostsAgain() throws Exception {
		createRent(owner, 10).andExpect(status().isCreated());
		job.run();

		clock.travelTo(Instant.parse("2026-04-10T00:05:00Z"));
		job.run();

		expenses().andExpect(jsonPath("$.content[*].expenseDate", contains("2026-04-10", "2026-03-10")));
	}

	@Test
	void dayAlreadyPassedThisMonthStartsNextMonth() throws Exception {
		createRent(owner, 5).andExpect(jsonPath("$.nextRunDate").value("2026-04-05"));

		job.run();

		expenses().andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void missedMonthsAreEachPostedWithTheirOwnDate() throws Exception {
		createRent(owner, 10).andExpect(status().isCreated());

		clock.travelTo(Instant.parse("2026-06-15T09:00:00Z"));
		job.run();

		expenses().andExpect(jsonPath("$.content[*].expenseDate",
				contains("2026-06-10", "2026-05-10", "2026-04-10", "2026-03-10")));
		recurringList().andExpect(jsonPath("$[0].nextRunDate").value("2026-07-10"));
	}

	@Test
	void stoppedRecurringExpenseIsNotPosted() throws Exception {
		long id = jsonLong(createRent(owner, 10), "$.id");

		deleteAs(owner, url() + "/" + id).andExpect(status().isNoContent());
		job.run();

		expenses().andExpect(jsonPath("$.totalElements").value(0));
		recurringList().andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void onlyTheOwnerCanStopOne() throws Exception {
		long id = jsonLong(createRent(member, 10), "$.id");

		deleteAs(member, url() + "/" + id).andExpect(status().isForbidden());
	}

	@Test
	void stoppingOneFromAnotherGroupIsNotFound() throws Exception {
		String stranger = registerAndLogin(uniqueEmail());
		long strangerId = userId(stranger);
		long otherGroup = createGroup(stranger, "Other flat");
		long foreign = jsonLong(postAs(stranger, "/api/groups/" + otherGroup + "/recurring-expenses", """
				{"description": "Wifi", "amountPaise": 100, "paidBy": %d, "dayOfMonth": 1}
				""".formatted(strangerId)).andExpect(status().isCreated()), "$.id");

		deleteAs(owner, url() + "/" + foreign).andExpect(status().isNotFound());
	}

	@Test
	void dayOfMonthMustBeBetween1And28() throws Exception {
		createRent(owner, 29).andExpect(status().isBadRequest());
		createRent(owner, 0).andExpect(status().isBadRequest());
	}

	@Test
	void payerMustBeAMember() throws Exception {
		long outsiderId = userId(registerAndLogin(uniqueEmail()));
		postAs(owner, url(), """
				{"description": "Rent", "amountPaise": 100, "paidBy": %d, "dayOfMonth": 10}
				""".formatted(outsiderId)).andExpect(status().isBadRequest());
	}

}
