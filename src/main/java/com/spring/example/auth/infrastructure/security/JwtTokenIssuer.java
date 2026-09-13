package com.spring.example.auth.infrastructure.security;

import com.spring.example.auth.domain.TokenIssuer;
import com.spring.example.auth.domain.User;
import com.spring.example.auth.infrastructure.config.JwtProperties;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
class JwtTokenIssuer implements TokenIssuer {

	static final String ISSUER = "self";

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	JwtTokenIssuer(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	@Override
	public IssuedToken issue(User user) {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER)
			.issuedAt(now)
			.expiresAt(now.plus(properties.ttlMinutes(), ChronoUnit.MINUTES))
			.subject(user.getUsername())
			.id(UUID.randomUUID().toString())
			.claim("uid", user.getId())
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(value, "Bearer", properties.ttlSeconds());
	}
}
