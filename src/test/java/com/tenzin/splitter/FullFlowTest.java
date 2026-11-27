package com.tenzin.splitter;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * The demo flow end to end against real PostgreSQL: register, log in, create a group,
 * invite friends, add expenses, read balances, get suggestions, settle, refresh, log out.
 */
@IntegrationTest
class FullFlowTest extends ApiTestSupport {

	@Test
	void goaTripFromSignUpToSettledUp() throws Exception {
		// Three people sign up and log in
		String youEmail = uniqueEmail();
		register(youEmail, "You", PASSWORD).andExpect(status().isCreated());
		var youLogin = login(youEmail, PASSWORD).andExpect(status().isOk());
		String you = json(youLogin, "$.accessToken");
		String youRefresh = json(youLogin, "$.refreshToken");
		String a = registerAndLogin(uniqueEmail());
		String b = registerAndLogin(uniqueEmail());
		long youId = userId(you);
		long aId = userId(a);
		long bId = userId(b);

		// You create the group and invite A and B with single-use links
		long group = createGroup(you, "Goa trip");
		addMember(you, group, a);
		addMember(you, group, b);
		getAs(b, "/api/groups/" + group).andExpect(jsonPath("$.members", hasSize(3)));

		// Three expenses: two equal splits and one exact split
		postAs(you, "/api/groups/" + group + "/expenses", """
				{"description": "Villa", "amountPaise": 300000, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d, %d, %d]}
				""".formatted(youId, youId, aId, bId)).andExpect(status().isCreated());
		postAs(a, "/api/groups/" + group + "/expenses", """
				{"description": "Scooters", "amountPaise": 90000, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d, %d, %d]}
				""".formatted(aId, youId, aId, bId)).andExpect(status().isCreated());
		postAs(b, "/api/groups/" + group + "/expenses", """
				{"description": "Dinner", "amountPaise": 60000, "paidBy": %d, "splitType": "EXACT",
				 "shares": [{"userId": %d, "sharePaise": 20000}, {"userId": %d, "sharePaise": 20000},
				            {"userId": %d, "sharePaise": 20000}]}
				""".formatted(bId, youId, aId, bId)).andExpect(status().isCreated());
		getAs(a, "/api/groups/" + group + "/expenses?size=2")
			.andExpect(jsonPath("$.totalElements").value(3))
			.andExpect(jsonPath("$.content", hasSize(2)));

		// Balances: +1,500 / -600 / -900 rupees
		getAs(a, "/api/groups/" + group + "/balances")
			.andExpect(jsonPath("$[?(@.userId == " + youId + ")].netPaise").value(150000))
			.andExpect(jsonPath("$[?(@.userId == " + aId + ")].netPaise").value(-60000))
			.andExpect(jsonPath("$[?(@.userId == " + bId + ")].netPaise").value(-90000));

		// Suggested: B pays you 900, A pays you 600
		getAs(you, "/api/groups/" + group + "/settlements/suggested")
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].fromUserId").value(bId))
			.andExpect(jsonPath("$[0].amountPaise").value(90000))
			.andExpect(jsonPath("$[1].fromUserId").value(aId))
			.andExpect(jsonPath("$[1].amountPaise").value(60000));

		// They pay, and record it
		postAs(b, "/api/groups/" + group + "/settlements", """
				{"toUserId": %d, "amountPaise": 90000}
				""".formatted(youId)).andExpect(status().isCreated());
		postAs(a, "/api/groups/" + group + "/settlements", """
				{"toUserId": %d, "amountPaise": 60000}
				""".formatted(youId)).andExpect(status().isCreated());
		getAs(you, "/api/groups/" + group + "/settlements/suggested").andExpect(jsonPath("$", hasSize(0)));

		// Session lifecycle: rotate the refresh token, then log out
		String rotated = json(refresh(youRefresh).andExpect(status().isOk()), "$.refreshToken");
		logout(rotated).andExpect(status().isNoContent());
		refresh(rotated).andExpect(status().isUnauthorized());
	}

	@Test
	void healthIsPublicForThePlatformHealthCheck() throws Exception {
		mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void apiDocsArePublicAndDescribeBearerAuth() throws Exception {
		mvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("/api/groups/{groupId}/settlements/suggested")))
			.andExpect(content().string(containsString("bearerAuth")));
	}

	@Test
	void swaggerUiIsPublic() throws Exception {
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void otherActuatorEndpointsAreNotExposed() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
	}

}
