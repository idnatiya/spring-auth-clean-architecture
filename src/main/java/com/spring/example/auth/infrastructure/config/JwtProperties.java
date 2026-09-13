package com.spring.example.auth.infrastructure.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32, message = "must be at least 32 bytes, set JWT_SECRET in .env") String secret,
		@Positive long ttlMinutes) {

	public long ttlSeconds() {
		return ttlMinutes * 60;
	}
}
