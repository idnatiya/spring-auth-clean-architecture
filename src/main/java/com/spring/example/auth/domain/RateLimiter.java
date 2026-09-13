package com.spring.example.auth.domain;

public interface RateLimiter {

	/** Throws {@link TooManyAttemptsException} once this key has used up its quota. */
	void check(String key, int limitPerMinute);
}
