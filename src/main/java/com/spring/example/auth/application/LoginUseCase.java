package com.spring.example.auth.application;

import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.InvalidCredentialsException;
import com.spring.example.auth.domain.PasswordHasher;
import com.spring.example.auth.domain.RateLimiter;
import com.spring.example.auth.domain.TooManyAttemptsException;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import com.spring.example.auth.domain.Usernames;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginUseCase {

	private static final Logger log = LoggerFactory.getLogger(LoginUseCase.class);

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

		// putCloseable: the MDC entry is removed even when this method throws. A plain put()
		// would leak the username onto the next request served by the same Tomcat thread.
		try (var ignored = MDC.putCloseable("username", username)) {
			// Checked before hitting the DB so brute force attempts do not also load the database.
			try {
				rateLimiter.check("login:" + username, rateLimits.login());
			}
			catch (TooManyAttemptsException ex) {
				log.warn("Login rate limit exceeded");
				throw ex;
			}
			log.debug("Rate limit passed, looking up user");

			Optional<User> found = users.findByUsername(username);
			if (found.isEmpty()) {
				// Still pay the hashing cost so response time does not reveal whether the user exists.
				passwordHasher.burn(rawPassword);
				// The HTTP response stays generic; the log may be specific. What must not leak is
				// information to the attacker, not to the operator reading Loki.
				log.warn("Login failed: no such user");
				throw new InvalidCredentialsException();
			}

			User user = found.get();
			try (var ignoredId = MDC.putCloseable("user_id", String.valueOf(user.getId()))) {
				log.debug("User found, verifying password");
				if (!passwordHasher.matches(rawPassword, user.getPassword())) {
					log.warn("Login failed: wrong password");
					throw new InvalidCredentialsException();
				}
				AuthTokens tokens = tokenIssuer.issue(user, UUID.randomUUID());
				log.info("Login succeeded");
				return tokens;
			}
		}
	}
}
