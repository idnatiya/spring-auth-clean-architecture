package com.spring.example.auth.presentation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class RateLimitTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private StringRedisTemplate redis;

	@BeforeEach
	void clearCounters() {
		redis.delete(redis.keys("rate:*"));
	}

	@Test
	void sixthLoginAttemptIsThrottled() throws Exception {
		String target = "korban-" + System.nanoTime();
		for (int attempt = 1; attempt <= 5; attempt++) {
			mvc.perform(login(target)).andExpect(status().isUnauthorized());
		}
		mvc.perform(login(target)).andExpect(status().isTooManyRequests());

		// the quota is counted per username, not globally
		mvc.perform(login("lain-" + System.nanoTime())).andExpect(status().isUnauthorized());
	}

	private static org.springframework.test.web.servlet.RequestBuilder login(String username) {
		return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("""
					{"username":"%s","password":"passwordsalah"}""".formatted(username));
	}
}
