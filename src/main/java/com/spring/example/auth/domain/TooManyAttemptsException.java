package com.spring.example.auth.domain;

public class TooManyAttemptsException extends RuntimeException {

	public TooManyAttemptsException() {
		super("Too many attempts, try again shortly");
	}
}
