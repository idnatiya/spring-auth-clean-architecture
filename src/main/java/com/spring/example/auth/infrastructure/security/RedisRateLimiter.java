package com.spring.example.auth.infrastructure.security;

import com.spring.example.auth.domain.RateLimiter;
import com.spring.example.auth.domain.TooManyAttemptsException;
import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * One minute fixed window.
 *
 * ponytail: fixed window, so a burst of 2x the limit is still possible right at the boundary
 * between two windows. Move to a sliding window (Lua script or Bucket4j) if that precision starts
 * to matter.
 */
@Component
class RedisRateLimiter implements RateLimiter {

	private static final Duration WINDOW = Duration.ofMinutes(1);

	private final StringRedisTemplate redis;

	RedisRateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	@Override
	public void check(String key, int limitPerMinute) {
		String redisKey = "rate:" + key;
		Long hits = redis.opsForValue().increment(redisKey);
		if (hits == null) {
			return;
		}
		if (hits == 1) {
			redis.expire(redisKey, WINDOW);
		}
		if (hits > limitPerMinute) {
			throw new TooManyAttemptsException();
		}
	}
}
