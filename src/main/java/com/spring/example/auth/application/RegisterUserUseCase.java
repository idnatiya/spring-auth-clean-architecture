package com.spring.example.auth.application;

import com.spring.example.auth.domain.PasswordHasher;
import com.spring.example.auth.domain.RateLimiter;
import com.spring.example.auth.domain.TooManyAttemptsException;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import com.spring.example.auth.domain.UsernameTakenException;
import com.spring.example.auth.domain.Usernames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterUserUseCase {

	private static final Logger log = LoggerFactory.getLogger(RegisterUserUseCase.class);

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
		String username = Usernames.normalize(rawUsername);

		try (var ignoredIp = MDC.putCloseable("client_ip", clientIp);
				var ignoredUser = MDC.putCloseable("username", username)) {
			// Usernames are always new when signup is spammed, so the key is the IP.
			try {
				rateLimiter.check("register:" + clientIp, rateLimits.register());
			}
			catch (TooManyAttemptsException ex) {
				log.warn("Registration rate limit exceeded");
				throw ex;
			}

			if (users.existsByUsername(username)) {
				log.warn("Registration rejected: username already taken");
				throw new UsernameTakenException(username);
			}

			User saved = users.save(new User(username, passwordHasher.hash(rawPassword)));
			try (var ignoredId = MDC.putCloseable("user_id", String.valueOf(saved.getId()))) {
				log.info("User registered");
			}
			return saved;
		}
	}
}
