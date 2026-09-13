package com.spring.example.auth.infrastructure.security;

import com.spring.example.auth.domain.PasswordHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class BCryptPasswordHasher implements PasswordHasher {

	/** BCrypt cost 10 hash of a random value. The content does not matter, the CPU cost of verifying it does. */
	private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

	private final PasswordEncoder encoder;

	BCryptPasswordHasher(PasswordEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String hashedPassword) {
		return encoder.matches(rawPassword, hashedPassword);
	}

	@Override
	public void burn(String rawPassword) {
		encoder.matches(rawPassword, DUMMY_HASH);
	}
}
