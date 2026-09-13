package com.spring.example.auth.domain;

public class UsernameTakenException extends RuntimeException {

	public UsernameTakenException(String username) {
		super("Username already taken: " + username);
	}
}
