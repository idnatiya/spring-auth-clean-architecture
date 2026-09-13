package com.spring.example.auth.infrastructure.persistence;

import com.spring.example.auth.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
	int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from RefreshToken t where t.expiresAt < :cutoff")
	int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
