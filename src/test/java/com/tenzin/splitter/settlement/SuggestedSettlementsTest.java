package com.tenzin.splitter.settlement;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** End to end: Goa trip expenses, then the suggestions, then paying them clears everything. */
@IntegrationTest
class SuggestedSettlementsTest extends ApiTestSupport {

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

	private void addEqualExpense(String token, long rupees, long payer) throws Exception {
		postAs(token, "/api/groups/" + groupId + "/expenses", """
				{"description": "Goa", "amountPaise": %d, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d, %d, %d]}
				""".formatted(rupees * 100, payer, youId, aId, bId)).andExpect(status().isCreated());
	}

	private String suggestedUrl() {
		return "/api/groups/" + groupId + "/settlements/suggested";
	}

	@Test
	void goaTripSuggestsBPaysYou900AndAPaysYou600() throws Exception {
		addEqualExpense(you, 3000, youId);
		addEqualExpense(a, 900, aId);
		addEqualExpense(b, 600, bId);

		getAs(b, suggestedUrl())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].fromUserId").value(bId))
			.andExpect(jsonPath("$[0].toUserId").value(youId))
			.andExpect(jsonPath("$[0].amountPaise").value(90_000))
			.andExpect(jsonPath("$[0].fromName").value("Test User"))
			.andExpect(jsonPath("$[1].fromUserId").value(aId))
			.andExpect(jsonPath("$[1].toUserId").value(youId))
			.andExpect(jsonPath("$[1].amountPaise").value(60_000));
	}

	@Test
	void payingTheSuggestionsLeavesNothingToSettle() throws Exception {
		addEqualExpense(you, 3000, youId);
		addEqualExpense(a, 900, aId);
		addEqualExpense(b, 600, bId);

		postAs(b, "/api/groups/" + groupId + "/settlements", """
				{"toUserId": %d, "amountPaise": 90000}
				""".formatted(youId)).andExpect(status().isCreated());
		postAs(a, "/api/groups/" + groupId + "/settlements", """
				{"toUserId": %d, "amountPaise": 60000}
				""".formatted(youId)).andExpect(status().isCreated());

		getAs(you, suggestedUrl()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void newGroupHasNothingToSettle() throws Exception {
		getAs(you, suggestedUrl()).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
	}

}
