package com.spring.example.auth.domain;

public class InvalidRefreshTokenException extends RuntimeException {

	public InvalidRefreshTokenException() {
		super("Invalid refresh token");
	}
}
