package com.spring.example.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Creates and hashes refresh tokens. Plain JDK, no framework. */
public final class RefreshTokenFactory {

	private static final SecureRandom RANDOM = new SecureRandom();

	private RefreshTokenFactory() {
	}

	/** The raw 256-bit value handed to the client. */
	public static String generate() {
		byte[] bytes = new byte[32];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** The hash stored in the DB. The raw token has full entropy, so plain SHA-256 is enough. */
	public static String hash(String rawToken) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("SHA-256 is required on every JVM", ex);
		}
	}
}
