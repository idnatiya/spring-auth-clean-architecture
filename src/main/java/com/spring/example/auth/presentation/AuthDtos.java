package com.spring.example.auth.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

	private AuthDtos() {
	}

	public record RegisterRequest(@NotBlank @Size(max = 64) String username,
			@NotBlank @Size(min = 8, max = 72) String password) {
	}

	public record LoginRequest(@NotBlank String username, @NotBlank String password) {
	}

	public record RefreshRequest(@NotBlank String refreshToken) {
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
			long refreshExpiresIn) {
	}

	public record UserResponse(Long id, String username) {
	}
}
