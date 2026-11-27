package com.tenzin.splitter.expense;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class ExpenseTest extends ApiTestSupport {

	private String owner;

	private String member;

	private String outsider;

	private long ownerId;

	private long memberId;

	private long outsiderId;

	private long groupId;

	@BeforeEach
	void setUpGroup() throws Exception {
		owner = registerAndLogin(uniqueEmail());
		member = registerAndLogin(uniqueEmail());
		outsider = registerAndLogin(uniqueEmail());
		ownerId = userId(owner);
		memberId = userId(member);
		outsiderId = userId(outsider);
		groupId = createGroup(owner, "Goa trip");
		addMember(owner, groupId, member);
	}

	private String expensesUrl() {
		return "/api/groups/" + groupId + "/expenses";
	}

	private ResultActions addEqual(String token, long amount, long paidBy, String date, long... participants)
			throws Exception {
		String ids = java.util.Arrays.toString(participants);
		return postAs(token, expensesUrl(), """
				{"description": "Dinner", "amountPaise": %d, "paidBy": %d, "splitType": "EQUAL",
				 "expenseDate": "%s", "participantIds": %s}
				""".formatted(amount, paidBy, date, ids));
	}

	@Test
	void equalSplitIsComputedByTheServer() throws Exception {
		addEqual(owner, 10_000, ownerId, "2026-01-10", memberId, ownerId)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.splitType").value("EQUAL"))
			.andExpect(jsonPath("$.createdBy").value(ownerId))
			.andExpect(jsonPath("$.shares", hasSize(2)))
			.andExpect(jsonPath("$.shares[*].sharePaise", contains(5000, 5000)));
	}

	@Test
	void exactSplitFromThePlanIsAccepted() throws Exception {
		String third = registerAndLogin(uniqueEmail());
		long thirdId = userId(third);
		addMember(owner, groupId, third);

		postAs(member, expensesUrl(), """
				{"description": "Dinner at Goa", "amountPaise": 90000, "paidBy": %d, "splitType": "EXACT",
				 "shares": [{"userId": %d, "sharePaise": 30000},
				            {"userId": %d, "sharePaise": 40000},
				            {"userId": %d, "sharePaise": 20000}]}
				""".formatted(ownerId, ownerId, memberId, thirdId))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.amountPaise").value(90000))
			.andExpect(jsonPath("$.createdBy").value(memberId))
			.andExpect(jsonPath("$.shares", hasSize(3)));
	}

	@Test
	void exactSplitThatDoesNotAddUpIsRejected() throws Exception {
		postAs(owner, expensesUrl(), """
				{"description": "Dinner", "amountPaise": 90000, "paidBy": %d, "splitType": "EXACT",
				 "shares": [{"userId": %d, "sharePaise": 30000}, {"userId": %d, "sharePaise": 40000}]}
				""".formatted(ownerId, ownerId, memberId))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("add up to 70000")));
	}

	@Test
	void nonMemberCannotBeThePayer() throws Exception {
		addEqual(owner, 1000, outsiderId, "2026-01-10", ownerId, memberId)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("must be members")));
	}

	@Test
	void nonMemberCannotHoldAShare() throws Exception {
		addEqual(owner, 1000, ownerId, "2026-01-10", ownerId, outsiderId).andExpect(status().isBadRequest());
	}

	@Test
	void mixingParticipantIdsAndSharesIsRejected() throws Exception {
		postAs(owner, expensesUrl(), """
				{"description": "Dinner", "amountPaise": 1000, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d], "shares": [{"userId": %d, "sharePaise": 1000}]}
				""".formatted(ownerId, ownerId, ownerId)).andExpect(status().isBadRequest());
	}

	@Test
	void zeroOrNegativeAmountFailsValidation() throws Exception {
		addEqual(owner, 0, ownerId, "2026-01-10", ownerId)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("amountPaise"));
	}

	@Test
	void listIsPaginatedNewestFirst() throws Exception {
		addEqual(owner, 100, ownerId, "2026-01-01", ownerId).andExpect(status().isCreated());
		addEqual(owner, 200, ownerId, "2026-03-01", ownerId).andExpect(status().isCreated());
		addEqual(owner, 300, ownerId, "2026-02-01", ownerId).andExpect(status().isCreated());

		getAs(member, expensesUrl() + "?page=0&size=2")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].amountPaise", contains(200, 300)))
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.totalPages").value(2));

		getAs(member, expensesUrl() + "?page=1&size=2").andExpect(jsonPath("$.content[*].amountPaise", contains(100)));
	}

	@Test
	void oversizedPageIsRejected() throws Exception {
		getAs(member, expensesUrl() + "?size=500").andExpect(status().isBadRequest());
	}

	@Test
	void creatorCanDeleteTheirOwnExpense() throws Exception {
		long id = jsonLong(addEqual(member, 1000, memberId, "2026-01-10", ownerId, memberId), "$.id");

		deleteAs(member, expensesUrl() + "/" + id).andExpect(status().isNoContent());
		getAs(member, expensesUrl()).andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	void ownerCanDeleteAnyExpenseInTheGroup() throws Exception {
		long id = jsonLong(addEqual(member, 1000, memberId, "2026-01-10", ownerId, memberId), "$.id");

		deleteAs(owner, expensesUrl() + "/" + id).andExpect(status().isNoContent());
	}

	@Test
	void otherMembersCannotDeleteSomeoneElsesExpense() throws Exception {
		long id = jsonLong(addEqual(owner, 1000, ownerId, "2026-01-10", ownerId, memberId), "$.id");

		deleteAs(member, expensesUrl() + "/" + id).andExpect(status().isForbidden());
	}

	/** IDOR through a group I do belong to: an expense id from another group must not resolve. */
	@Test
	void expenseFromAnotherGroupIsNotFoundThroughMyGroupsUrl() throws Exception {
		long otherGroup = createGroup(outsider, "Outsider's flat");
		long foreignExpense = jsonLong(postAs(outsider, "/api/groups/" + otherGroup + "/expenses", """
				{"description": "Rent", "amountPaise": 5000, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d]}
				""".formatted(outsiderId, outsiderId)).andExpect(status().isCreated()), "$.id");

		deleteAs(owner, expensesUrl() + "/" + foreignExpense).andExpect(status().isNotFound());
		getAs(outsider, "/api/groups/" + otherGroup + "/expenses").andExpect(jsonPath("$.totalElements").value(1));
	}

}
