package com.spring.example.auth.presentation;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper mapper;

	private static final String PASSWORD = "rahasia123";

	@Test
	void registerLoginAccessProtectedEndpoint() throws Exception {
		String username = "andi-" + System.nanoTime();
		String credentials = """
				{"username":"%s","password":"%s"}""".formatted(username, PASSWORD);

		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.username").value(username));

		// same username -> 409
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
			.andExpect(status().isConflict());

		// wrong password -> 401
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"username":"%s","password":"passwordsalah"}""".formatted(username)))
			.andExpect(status().isUnauthorized());

		String body = mvc
			.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").value(notNullValue()))
			.andExpect(jsonPath("$.refreshToken").value(notNullValue()))
			.andExpect(jsonPath("$.expiresIn").value(900))
			.andReturn()
			.getResponse()
			.getContentAsString();
		JsonNode token = mapper.readTree(body);

		// without a token -> 401
		mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());

		mvc.perform(get("/api/me").header("Authorization", "Bearer " + token.get("accessToken").asText()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(username));
	}

	@Test
	void registerRejectsShortPassword() throws Exception {
		mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"username":"pendek","password":"123"}"""))
			.andExpect(status().isBadRequest());
	}
}
