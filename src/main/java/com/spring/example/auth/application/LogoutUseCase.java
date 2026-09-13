package com.spring.example.auth.application;

import com.spring.example.auth.domain.RefreshTokenFactory;
import com.spring.example.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

	private static final Logger log = LoggerFactory.getLogger(LogoutUseCase.class);

	private final RefreshTokenRepository refreshTokens;

	public LogoutUseCase(RefreshTokenRepository refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	/** Stay silent when the token is not found, so this cannot be used to probe token validity. */
	@Transactional
	public void execute(String rawToken) {
		Instant now = Instant.now();
		String tokenHash = RefreshTokenFactory.hash(rawToken);

		try (var ignored = MDC.putCloseable("token_hash_prefix", LogFields.hashPrefix(tokenHash))) {
			refreshTokens.findByTokenHash(tokenHash).ifPresentOrElse(token -> {
				refreshTokens.revokeFamily(token.getFamilyId(), now);
				try (var ignoredUser = MDC.putCloseable("username", token.getUsername());
						var ignoredFamily = MDC.putCloseable("token_family", token.getFamilyId().toString())) {
					log.info("Logged out, refresh token family revoked");
				}
			}, () -> log.debug("Logout with unknown token, nothing revoked"));
		}
	}
}
