package com.spring.example.auth.infrastructure.persistence;

import com.spring.example.auth.domain.RefreshToken;
import com.spring.example.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class JpaRefreshTokenRepository implements RefreshTokenRepository {

	private final SpringDataRefreshTokenRepository delegate;

	JpaRefreshTokenRepository(SpringDataRefreshTokenRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public RefreshToken save(RefreshToken token) {
		return delegate.save(token);
	}

	@Override
	public Optional<RefreshToken> findByTokenHash(String tokenHash) {
		return delegate.findByTokenHash(tokenHash);
	}

	@Override
	public void revokeFamily(UUID familyId, Instant revokedAt) {
		delegate.revokeFamily(familyId, revokedAt);
	}

	@Override
	public int deleteExpiredBefore(Instant cutoff) {
		return delegate.deleteExpiredBefore(cutoff);
	}
}
