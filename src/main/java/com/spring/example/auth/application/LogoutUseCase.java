package com.spring.example.auth.application;

import com.spring.example.auth.domain.RefreshTokenFactory;
import com.spring.example.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogoutUseCase {

	private final RefreshTokenRepository refreshTokens;

	public LogoutUseCase(RefreshTokenRepository refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	/** Stay silent when the token is not found, so this cannot be used to probe token validity. */
	@Transactional
	public void execute(String rawToken) {
		Instant now = Instant.now();
		refreshTokens.findByTokenHash(RefreshTokenFactory.hash(rawToken))
			.ifPresent(token -> refreshTokens.revokeFamily(token.getFamilyId(), now));
	}
}
