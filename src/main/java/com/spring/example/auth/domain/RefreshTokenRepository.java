package com.spring.example.auth.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

	RefreshToken save(RefreshToken token);

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	void revokeFamily(UUID familyId, Instant revokedAt);

	int deleteExpiredBefore(Instant cutoff);
}
