package com.spring.example.auth.application;

import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.InvalidRefreshTokenException;
import com.spring.example.auth.domain.RefreshToken;
import com.spring.example.auth.domain.RefreshTokenFactory;
import com.spring.example.auth.domain.RefreshTokenRepository;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.domain.UserRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenUseCase {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenUseCase.class);

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
		String tokenHash = RefreshTokenFactory.hash(rawToken);

		// Only a prefix of the hash: enough to match log lines to each other, useless as a token.
		try (var ignored = MDC.putCloseable("token_hash_prefix", LogFields.hashPrefix(tokenHash))) {
			RefreshToken stored = refreshTokens.findByTokenHash(tokenHash).orElseThrow(() -> {
				log.warn("Refresh rejected: unknown token");
				return new InvalidRefreshTokenException();
			});

			try (var ignoredUser = MDC.putCloseable("username", stored.getUsername());
					var ignoredFamily = MDC.putCloseable("token_family", stored.getFamilyId().toString())) {

				log.debug("Refresh token found, revoked={} expiresAt={}", stored.isRevoked(), stored.getExpiresAt());

				if (stored.isRevoked()) {
					// An already rotated token was used again: a copy leaked. Revoke the whole chain,
					// including the newest token an attacker may be holding.
					refreshTokens.revokeFamily(stored.getFamilyId(), now);
					log.warn("Refresh token reuse detected, whole family revoked");
					throw new InvalidRefreshTokenException();
				}
				if (stored.isExpired(now)) {
					log.warn("Refresh rejected: token expired");
					throw new InvalidRefreshTokenException();
				}

				stored.revoke(now);
				refreshTokens.save(stored);

				User user = users.findByUsername(stored.getUsername()).orElseThrow(() -> {
					log.warn("Refresh rejected: owner no longer exists");
					return new InvalidRefreshTokenException();
				});
				AuthTokens tokens = tokenIssuer.issue(user, stored.getFamilyId());
				log.info("Refresh token rotated");
				return tokens;
			}
		}
	}
}
