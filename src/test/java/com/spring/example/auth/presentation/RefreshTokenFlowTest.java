package com.spring.example.auth.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class RefreshTokenFlowTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	private static final String PASSWORD = "rahasia123";

	@Test
	void rotateReuseDetectionAndLogout() throws Exception {
		String username = "rotasi-" + System.nanoTime();
		JsonNode first = registerAndLogin(username);
		String refresh1 = first.get("refreshToken").asString();

		JsonNode second = mapper.readTree(refresh(refresh1, status().isOk()));
		String refresh2 = second.get("refreshToken").asString();
		assertThat(refresh2).isNotEqualTo(refresh1);

		// the access token from the rotation is still valid
		mvc.perform(get("/api/me").header("Authorization", "Bearer " + second.get("accessToken").asString()))
			.andExpect(status().isOk());

		// the old token reused -> rejected, AND the whole family is revoked with it
		refresh(refresh1, status().isUnauthorized());
		refresh(refresh2, status().isUnauthorized());
	}

	@Test
	void logoutRevokesRefreshToken() throws Exception {
		String username = "logout-" + System.nanoTime();
		String refresh = registerAndLogin(username).get("refreshToken").asString();

		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body(refresh)))
			.andExpect(status().isNoContent());

		refresh(refresh, status().isUnauthorized());
	}

	@Test
	void unknownRefreshTokenRejected() throws Exception {
		refresh("token-karangan", status().isUnauthorized());
	}

	private JsonNode registerAndLogin(String username) throws Exception {
		String credentials = """
				{"username":"%s","password":"%s"}""".formatted(username, PASSWORD);
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
			.andExpect(status().isCreated());
		String body = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString();
		return mapper.readTree(body);
	}

	private String refresh(String token,
			org.springframework.test.web.servlet.ResultMatcher expected) throws Exception {
		return mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body(token)))
			.andExpect(expected)
			.andReturn()
			.getResponse()
			.getContentAsString();
	}

	private static String body(String token) {
		return """
				{"refreshToken":"%s"}""".formatted(token);
	}
}
