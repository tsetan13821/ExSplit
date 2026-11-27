package com.tenzin.splitter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Helpers for integration tests. The database is shared across tests, so every user
 * gets a unique email instead of relying on cleanup between tests.
 */
public abstract class ApiTestSupport {

	protected static final String PASSWORD = "correct-horse-battery";

	@Autowired
	protected MockMvc mvc;

	protected static String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}

	protected ResultActions postJson(String url, String json) throws Exception {
		return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	protected ResultActions register(String email, String name, String password) throws Exception {
		return postJson("/api/auth/register", """
				{"email": "%s", "name": "%s", "password": "%s"}
				""".formatted(email, name, password));
	}

	protected ResultActions login(String email, String password) throws Exception {
		return postJson("/api/auth/login", """
				{"email": "%s", "password": "%s"}
				""".formatted(email, password));
	}

	protected ResultActions refresh(String refreshToken) throws Exception {
		return postJson("/api/auth/refresh", """
				{"refreshToken": "%s"}
				""".formatted(refreshToken));
	}

	protected ResultActions logout(String refreshToken) throws Exception {
		return postJson("/api/auth/logout", """
				{"refreshToken": "%s"}
				""".formatted(refreshToken));
	}

	/** Reads a string field from a JSON response. */
	protected static String json(ResultActions result, String path) throws Exception {
		return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
	}

	/** Registers a fresh user and returns a valid access token for them. */
	protected String registerAndLogin(String email) throws Exception {
		register(email, "Test User", PASSWORD).andExpect(status().isCreated());
		String body = login(email, PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.accessToken");
	}

	protected static String bearer(String token) {
		return "Bearer " + token;
	}

	protected ResultActions getAs(String token, String url) throws Exception {
		return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	protected ResultActions postAs(String token, String url, String json) throws Exception {
		return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(json));
	}

	protected ResultActions postAs(String token, String url) throws Exception {
		return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	protected ResultActions deleteAs(String token, String url) throws Exception {
		return mvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	/** Reads a numeric field (ids) from a JSON response. */
	protected static long jsonLong(ResultActions result, String path) throws Exception {
		Number value = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
		return value.longValue();
	}

	/** Creates a group owned by the token's user and returns its id. */
	protected long createGroup(String ownerToken, String name) throws Exception {
		return jsonLong(postAs(ownerToken, "/api/groups", """
				{"name": "%s"}
				""".formatted(name)).andExpect(status().isCreated()), "$.id");
	}

	/** Owner creates an invite; returns the raw invite token. */
	protected String createInvite(String ownerToken, long groupId) throws Exception {
		return json(postAs(ownerToken, "/api/groups/" + groupId + "/invites").andExpect(status().isCreated()),
				"$.token");
	}

	/** Puts the member into the group through a real invite. */
	protected void addMember(String ownerToken, long groupId, String memberToken) throws Exception {
		String invite = createInvite(ownerToken, groupId);
		postAs(memberToken, "/api/invites/" + invite + "/accept").andExpect(status().isOk());
	}

}
