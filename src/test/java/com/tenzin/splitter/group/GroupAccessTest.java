package com.tenzin.splitter.group;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.stream.Stream;

import com.tenzin.splitter.ApiTestSupport;
import com.tenzin.splitter.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Phase 3 "done when": a non-member gets 403 on every group endpoint, one test each.
 * New group endpoints get a row in {@link #memberEndpoints()} or {@link #ownerEndpoints()}.
 */
@IntegrationTest
class GroupAccessTest extends ApiTestSupport {

	/**
	 * Placeholders: {g} group id, {owner} and {member} user ids, {other} the group member who
	 * is not the caller, {expense} an expense and {recurring} a recurring expense created by the owner. Bodies are valid for an
	 * authorised caller, so the only thing that can fail is authorization.
	 */
	record Endpoint(HttpMethod method, String path, String body) {

		Endpoint(HttpMethod method, String path) {
			this(method, path, null);
		}

		@Override
		public String toString() {
			return method + " " + path;
		}

	}

	static Stream<Endpoint> memberEndpoints() {
		return Stream.of(
				new Endpoint(HttpMethod.GET, "/api/groups/{g}"),
				new Endpoint(HttpMethod.GET, "/api/groups/{g}/expenses"),
				new Endpoint(HttpMethod.POST, "/api/groups/{g}/expenses", """
						{"description": "Dinner", "amountPaise": 1000, "paidBy": {owner},
						 "splitType": "EQUAL", "participantIds": [{owner}, {member}]}
						"""),
				new Endpoint(HttpMethod.GET, "/api/groups/{g}/balances"),
				new Endpoint(HttpMethod.GET, "/api/groups/{g}/settlements/suggested"),
				new Endpoint(HttpMethod.POST, "/api/groups/{g}/settlements", """
						{"toUserId": {other}, "amountPaise": 100}
						"""),
				new Endpoint(HttpMethod.GET, "/api/groups/{g}/recurring-expenses"),
				new Endpoint(HttpMethod.POST, "/api/groups/{g}/recurring-expenses", """
						{"description": "Rent", "amountPaise": 1000, "paidBy": {owner}, "dayOfMonth": 15}
						"""));
	}

	/** Owner-only, or (for the delete) owner-only here because the owner created that expense. */
	static Stream<Endpoint> ownerEndpoints() {
		return Stream.of(
				new Endpoint(HttpMethod.POST, "/api/groups/{g}/invites"),
				new Endpoint(HttpMethod.DELETE, "/api/groups/{g}/expenses/{expense}"),
				new Endpoint(HttpMethod.DELETE, "/api/groups/{g}/recurring-expenses/{recurring}"));
	}

	static Stream<Endpoint> allGroupEndpoints() {
		return Stream.concat(memberEndpoints(), ownerEndpoints());
	}

	private String ownerToken;

	private String memberToken;

	private String outsiderToken;

	private long groupId;

	private long ownerId;

	private long memberId;

	private long expenseId;

	private long recurringId;

	@BeforeEach
	void setUpGroup() throws Exception {
		ownerToken = registerAndLogin(uniqueEmail());
		memberToken = registerAndLogin(uniqueEmail());
		outsiderToken = registerAndLogin(uniqueEmail());
		groupId = createGroup(ownerToken, "Goa trip");
		addMember(ownerToken, groupId, memberToken);
		ownerId = userId(ownerToken);
		memberId = userId(memberToken);
		expenseId = jsonLong(postAs(ownerToken, "/api/groups/" + groupId + "/expenses", """
				{"description": "Taxi", "amountPaise": 500, "paidBy": %d, "splitType": "EQUAL",
				 "participantIds": [%d]}
				""".formatted(ownerId, ownerId)), "$.id");
		recurringId = jsonLong(postAs(ownerToken, "/api/groups/" + groupId + "/recurring-expenses", """
				{"description": "Wifi", "amountPaise": 900, "paidBy": %d, "dayOfMonth": 1}
				""".formatted(ownerId)), "$.id");
	}

	private String fill(String template, long group, String token) {
		long other = ownerToken.equals(token) ? memberId : ownerId;
		return template.replace("{g}", Long.toString(group))
			.replace("{owner}", Long.toString(ownerId))
			.replace("{member}", Long.toString(memberId))
			.replace("{other}", Long.toString(other))
			.replace("{expense}", Long.toString(expenseId))
			.replace("{recurring}", Long.toString(recurringId));
	}

	private ResultActions call(Endpoint endpoint, long group, String token) throws Exception {
		MockHttpServletRequestBuilder builder = request(endpoint.method(), fill(endpoint.path(), group, token));
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
		}
		if (endpoint.body() != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(fill(endpoint.body(), group, token));
		}
		return mvc.perform(builder);
	}

	@ParameterizedTest(name = "non-member gets 403: {0}")
	@MethodSource("allGroupEndpoints")
	void nonMemberIsForbidden(Endpoint endpoint) throws Exception {
		call(endpoint, groupId, outsiderToken)
			.andExpect(status().isForbidden())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(403));
	}

	@ParameterizedTest(name = "missing group looks the same as someone else's: {0}")
	@MethodSource("allGroupEndpoints")
	void nonexistentGroupIsForbiddenNotNotFound(Endpoint endpoint) throws Exception {
		call(endpoint, Long.MAX_VALUE, outsiderToken).andExpect(status().isForbidden());
	}

	@ParameterizedTest(name = "no token gets 401: {0}")
	@MethodSource("allGroupEndpoints")
	void anonymousIsUnauthorized(Endpoint endpoint) throws Exception {
		call(endpoint, groupId, null).andExpect(status().isUnauthorized());
	}

	@ParameterizedTest(name = "plain member gets 403 on owner-only: {0}")
	@MethodSource("ownerEndpoints")
	void memberIsForbiddenOnOwnerEndpoints(Endpoint endpoint) throws Exception {
		call(endpoint, groupId, memberToken).andExpect(status().isForbidden());
	}

	@ParameterizedTest(name = "member allowed: {0}")
	@MethodSource("memberEndpoints")
	void memberIsAllowed(Endpoint endpoint) throws Exception {
		call(endpoint, groupId, memberToken).andExpect(status().is2xxSuccessful());
	}

	@ParameterizedTest(name = "owner allowed: {0}")
	@MethodSource("allGroupEndpoints")
	void ownerIsAllowed(Endpoint endpoint) throws Exception {
		call(endpoint, groupId, ownerToken).andExpect(status().is2xxSuccessful());
	}

}
