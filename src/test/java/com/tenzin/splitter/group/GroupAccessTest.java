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

	/** {@code {g}} is replaced with the group id. Bodies are valid so only authorization can fail. */
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
				new Endpoint(HttpMethod.GET, "/api/groups/{g}"));
	}

	static Stream<Endpoint> ownerEndpoints() {
		return Stream.of(
				new Endpoint(HttpMethod.POST, "/api/groups/{g}/invites"));
	}

	static Stream<Endpoint> allGroupEndpoints() {
		return Stream.concat(memberEndpoints(), ownerEndpoints());
	}

	private String ownerToken;

	private String memberToken;

	private String outsiderToken;

	private long groupId;

	@BeforeEach
	void setUpGroup() throws Exception {
		ownerToken = registerAndLogin(uniqueEmail());
		memberToken = registerAndLogin(uniqueEmail());
		outsiderToken = registerAndLogin(uniqueEmail());
		groupId = createGroup(ownerToken, "Goa trip");
		addMember(ownerToken, groupId, memberToken);
	}

	private ResultActions call(Endpoint endpoint, long group, String token) throws Exception {
		MockHttpServletRequestBuilder builder = request(endpoint.method(),
				endpoint.path().replace("{g}", Long.toString(group)));
		if (token != null) {
			builder.header(HttpHeaders.AUTHORIZATION, bearer(token));
		}
		if (endpoint.body() != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(endpoint.body());
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
