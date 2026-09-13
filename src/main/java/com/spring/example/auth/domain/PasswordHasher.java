package com.spring.example.auth.domain;

/** Password hashing port. */
public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String hashedPassword);

	/**
	 * Hash against a dummy value. Called when the username is not found so that login response
	 * time matches the wrong-password case (timing attack defence).
	 */
	void burn(String rawPassword);
}
