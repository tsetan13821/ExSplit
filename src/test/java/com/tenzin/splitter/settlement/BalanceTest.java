package com.tenzin.splitter.settlement;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.ResultActions;

/**
 * Phase 4 "done when": the Goa example gives +1,500 / -600 / -900.
 * You paid ₹3,000, A paid ₹900, B paid ₹600, each split equally three ways.
 */
@IntegrationTest
class BalanceTest extends ApiTestSupport {

	private String you;

	private String a;

	private String b;

	private long youId;

	private long aId;

	private long bId;

	private long groupId;

	@BeforeEach
	void setUpGoaTrip() throws Exception {
		you = registerAndLogin(uniqueEmail());
		a = registerAndLogin(uniqueEmail());
		b = registerAndLogin(uniqueEmail());
		youId = userId(you);
		aId = userId(a);
		bId = userId(b);
		groupId = createGroup(you, "Goa trip");
		addMember(you, groupId, a);
		addMember(you, groupId, b);
	}

	private long addEqualExpense(String token, long rupees, long payer) throws Exception {
		return jsonLong(postAs(token, "/api/groups/" + groupId + "/expenses", """
				{"description": "Goa", "amountPaise": %d, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d, %d, %d]}
				""".formatted(rupees * 100, payer, youId, aId, bId)).andExpect(status().isCreated()), "$.id");
	}

	private ResultActions balances() throws Exception {
		return getAs(a, "/api/groups/" + groupId + "/balances").andExpect(status().isOk());
	}

	private ResultActions expectNet(ResultActions result, long user, long rupees) throws Exception {
		return result.andExpect(jsonPath("$[?(@.userId == " + user + ")].netPaise").value((int) (rupees * 100)));
	}

	private ResultActions settle(String payerToken, long to, long rupees) throws Exception {
		return postAs(payerToken, "/api/groups/" + groupId + "/settlements", """
				{"toUserId": %d, "amountPaise": %d}
				""".formatted(to, rupees * 100));
	}

	@Test
	void goaTripBalances() throws Exception {
		addEqualExpense(you, 3000, youId);
		addEqualExpense(a, 900, aId);
		addEqualExpense(b, 600, bId);

		ResultActions result = balances();
		expectNet(result, youId, 1500);
		expectNet(result, aId, -600);
		expectNet(result, bId, -900);
	}

	@Test
	void everyMemberIsListedEvenWithNoExpenses() throws Exception {
		ResultActions result = balances();
		expectNet(result, youId, 0);
		expectNet(result, aId, 0);
		expectNet(result, bId, 0);
	}

	@Test
	void settlementsMoveBalancesTowardsZero() throws Exception {
		addEqualExpense(you, 3000, youId);
		addEqualExpense(a, 900, aId);
		addEqualExpense(b, 600, bId);

		settle(b, youId, 900).andExpect(status().isCreated())
			.andExpect(jsonPath("$.fromUserId").value(bId))
			.andExpect(jsonPath("$.toUserId").value(youId));
		settle(a, youId, 600).andExpect(status().isCreated());

		ResultActions result = balances();
		expectNet(result, youId, 0);
		expectNet(result, aId, 0);
		expectNet(result, bId, 0);
	}

	@Test
	void deletingAnExpenseUndoesItsEffect() throws Exception {
		addEqualExpense(you, 3000, youId);
		long dinner = addEqualExpense(a, 900, aId);

		deleteAs(a, "/api/groups/" + groupId + "/expenses/" + dinner).andExpect(status().isNoContent());

		ResultActions result = balances();
		expectNet(result, youId, 2000);
		expectNet(result, aId, -1000);
		expectNet(result, bId, -1000);
	}

	@Test
	void cannotSettleWithYourself() throws Exception {
		settle(you, youId, 10).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail", containsString("yourself")));
	}

	@Test
	void cannotSettleWithANonMember() throws Exception {
		long outsiderId = userId(registerAndLogin(uniqueEmail()));
		settle(you, outsiderId, 10).andExpect(status().isBadRequest());
	}

	@Test
	void settlementAmountMustBePositive() throws Exception {
		settle(you, aId, 0).andExpect(status().isBadRequest());
	}

}
