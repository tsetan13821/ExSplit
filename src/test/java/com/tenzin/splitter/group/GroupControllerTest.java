package com.tenzin.splitter.group;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.Test;

@IntegrationTest
class GroupControllerTest extends ApiTestSupport {

	@Test
	void creatorBecomesOwnerAndOnlyMember() throws Exception {
		String email = uniqueEmail();
		String token = registerAndLogin(email);

		postAs(token, "/api/groups", """
				{"name": "  Goa trip  "}
				""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.name").value("Goa trip"))
			.andExpect(jsonPath("$.members", hasSize(1)))
			.andExpect(jsonPath("$.members[0].email").value(email))
			.andExpect(jsonPath("$.members[0].role").value("OWNER"));
	}

	@Test
	void blankNameIsRejected() throws Exception {
		String token = registerAndLogin(uniqueEmail());

		postAs(token, "/api/groups", """
				{"name": "  "}
				""").andExpect(status().isBadRequest());
	}

	@Test
	void listShowsOnlyMyGroupsWithMyRole() throws Exception {
		String owner = registerAndLogin(uniqueEmail());
		String member = registerAndLogin(uniqueEmail());
		String stranger = registerAndLogin(uniqueEmail());
		long goa = createGroup(owner, "Goa trip");
		long flat = createGroup(owner, "Flat 4B");
		createGroup(stranger, "Someone else's group");
		addMember(owner, goa, member);

		getAs(owner, "/api/groups")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) flat, (int) goa)))
			.andExpect(jsonPath("$[*].role", contains("OWNER", "OWNER")));

		getAs(member, "/api/groups")
			.andExpect(jsonPath("$[*].id", contains((int) goa)))
			.andExpect(jsonPath("$[0].role").value("MEMBER"));
	}

	@Test
	void detailsListMembersInJoinOrder() throws Exception {
		String owner = registerAndLogin(uniqueEmail());
		String memberEmail = uniqueEmail();
		String member = registerAndLogin(memberEmail);
		long group = createGroup(owner, "Goa trip");
		addMember(owner, group, member);

		getAs(member, "/api/groups/" + group)
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.members", hasSize(2)))
			.andExpect(jsonPath("$.members[*].role", contains("OWNER", "MEMBER")))
			.andExpect(jsonPath("$.members[1].email").value(memberEmail));
	}

}
