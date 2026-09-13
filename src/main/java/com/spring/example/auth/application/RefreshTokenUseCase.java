package com.spring.example.auth.application;

import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.InvalidRefreshTokenException;
import com.spring.example.auth.domain.RefreshToken;
import com.spring.example.auth.domain.RefreshTokenFactory;
import com.spring.example.auth.domain.RefreshTokenRepository;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

	private final RefreshTokenRepository refreshTokens;

	private final UserRepository users;

	private final AuthTokenIssuer tokenIssuer;

	public RefreshTokenUseCase(RefreshTokenRepository refreshTokens, UserRepository users,
			AuthTokenIssuer tokenIssuer) {
		this.refreshTokens = refreshTokens;
		this.users = users;
		this.tokenIssuer = tokenIssuer;
	}

	// noRollbackFor: the family revocation must still commit even though this method ends in an exception.
	@Transactional(noRollbackFor = InvalidRefreshTokenException.class)
	public AuthTokens execute(String rawToken) {
		Instant now = Instant.now();
		RefreshToken stored = refreshTokens.findByTokenHash(RefreshTokenFactory.hash(rawToken))
			.orElseThrow(InvalidRefreshTokenException::new);

		if (stored.isRevoked()) {
			// An already rotated token was used again: a copy leaked. Revoke the whole chain,
			// including the newest token an attacker may be holding.
			refreshTokens.revokeFamily(stored.getFamilyId(), now);
			throw new InvalidRefreshTokenException();
		}
		if (stored.isExpired(now)) {
			throw new InvalidRefreshTokenException();
		}

		stored.revoke(now);
		refreshTokens.save(stored);

		User user = users.findByUsername(stored.getUsername()).orElseThrow(InvalidRefreshTokenException::new);
		return tokenIssuer.issue(user, stored.getFamilyId());
	}
}
