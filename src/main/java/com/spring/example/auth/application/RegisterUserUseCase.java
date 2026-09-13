package com.spring.example.auth.application;

import com.spring.example.auth.domain.PasswordHasher;
import com.spring.example.auth.domain.RateLimiter;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import com.spring.example.auth.domain.UsernameTakenException;
import com.spring.example.auth.domain.Usernames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

	private final UserRepository users;

	private final PasswordHasher passwordHasher;

	private final RateLimiter rateLimiter;

	private final RateLimitProperties rateLimits;

	public RegisterUserUseCase(UserRepository users, PasswordHasher passwordHasher, RateLimiter rateLimiter,
			RateLimitProperties rateLimits) {
		this.users = users;
		this.passwordHasher = passwordHasher;
		this.rateLimiter = rateLimiter;
		this.rateLimits = rateLimits;
	}

	@Transactional
	public User execute(String rawUsername, String rawPassword, String clientIp) {
		// Usernames are always new when signup is spammed, so the key is the IP.
		rateLimiter.check("register:" + clientIp, rateLimits.register());

		String username = Usernames.normalize(rawUsername);
		if (users.existsByUsername(username)) {
			throw new UsernameTakenException(username);
		}
		return users.save(new User(username, passwordHasher.hash(rawPassword)));
	}
}
