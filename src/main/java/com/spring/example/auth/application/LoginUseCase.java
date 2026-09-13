package com.spring.example.auth.application;

import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.InvalidCredentialsException;
import com.spring.example.auth.domain.PasswordHasher;
import com.spring.example.auth.domain.RateLimiter;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import com.spring.example.auth.domain.Usernames;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

	private final UserRepository users;

	private final PasswordHasher passwordHasher;

	private final AuthTokenIssuer tokenIssuer;

	private final RateLimiter rateLimiter;

	private final RateLimitProperties rateLimits;

	public LoginUseCase(UserRepository users, PasswordHasher passwordHasher, AuthTokenIssuer tokenIssuer,
			RateLimiter rateLimiter, RateLimitProperties rateLimits) {
		this.users = users;
		this.passwordHasher = passwordHasher;
		this.tokenIssuer = tokenIssuer;
		this.rateLimiter = rateLimiter;
		this.rateLimits = rateLimits;
	}

	@Transactional
	public AuthTokens execute(String rawUsername, String rawPassword) {
		String username = Usernames.normalize(rawUsername);
		// Checked before hitting the DB so brute force attempts do not also load the database.
		rateLimiter.check("login:" + username, rateLimits.login());

		Optional<User> found = users.findByUsername(username);
		if (found.isEmpty()) {
			// Still pay the hashing cost so response time does not reveal whether the user exists.
			passwordHasher.burn(rawPassword);
			throw new InvalidCredentialsException();
		}
		User user = found.get();
		if (!passwordHasher.matches(rawPassword, user.getPassword())) {
			throw new InvalidCredentialsException();
		}
		return tokenIssuer.issue(user, UUID.randomUUID());
	}
}
