package com.spring.example.auth.application;

import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.RefreshToken;
import com.spring.example.auth.domain.RefreshTokenFactory;
import com.spring.example.auth.domain.RefreshTokenRepository;
import com.spring.example.auth.domain.TokenIssuer;
import com.spring.example.auth.domain.User;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Assembles the access + refresh token pair. Shared by login and rotation. */
@Component
class AuthTokenIssuer {

	private final TokenIssuer tokenIssuer;

	private final RefreshTokenRepository refreshTokens;

	private final Duration refreshTtl;

	AuthTokenIssuer(TokenIssuer tokenIssuer, RefreshTokenRepository refreshTokens,
			@Value("${app.jwt.refresh-ttl-days}") long refreshTtlDays) {
		this.tokenIssuer = tokenIssuer;
		this.refreshTokens = refreshTokens;
		this.refreshTtl = Duration.ofDays(refreshTtlDays);
	}

	AuthTokens issue(User user, UUID familyId) {
		String raw = RefreshTokenFactory.generate();
		Instant expiresAt = Instant.now().plus(refreshTtl);
		refreshTokens.save(new RefreshToken(RefreshTokenFactory.hash(raw), user.getUsername(), familyId, expiresAt));
		return new AuthTokens(tokenIssuer.issue(user), raw, refreshTtl.toSeconds());
	}
}
