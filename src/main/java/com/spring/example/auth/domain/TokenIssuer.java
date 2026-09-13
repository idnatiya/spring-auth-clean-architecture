package com.spring.example.auth.domain;

/** Port for issuing access tokens. */
public interface TokenIssuer {

	IssuedToken issue(User user);

	record IssuedToken(String value, String type, long expiresIn) {
	}
}
