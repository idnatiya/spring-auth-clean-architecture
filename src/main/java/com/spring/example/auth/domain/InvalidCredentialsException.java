package com.spring.example.auth.domain;

public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid credentials");
	}
}
