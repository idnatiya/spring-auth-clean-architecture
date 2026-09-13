package com.spring.example.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Opaque refresh token. Only the hash is stored; the raw value exists only in the HTTP response. */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_family", columnList = "familyId"))
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String tokenHash;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false)
	private UUID familyId;

	@Column(nullable = false)
	private Instant expiresAt;

	private Instant revokedAt;

	@Column(nullable = false)
	private Instant createdAt = Instant.now();

	protected RefreshToken() {
	}

	public RefreshToken(String tokenHash, String username, UUID familyId, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.username = username;
		this.familyId = familyId;
		this.expiresAt = expiresAt;
	}

	public String getUsername() {
		return username;
	}

	public UUID getFamilyId() {
		return familyId;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(Instant now) {
		return expiresAt.isBefore(now);
	}

	public void revoke(Instant now) {
		if (revokedAt == null) {
			this.revokedAt = now;
		}
	}
}
