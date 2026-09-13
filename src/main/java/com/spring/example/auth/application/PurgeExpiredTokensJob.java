package com.spring.example.auth.application;

import com.spring.example.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PurgeExpiredTokensJob {

	private static final Logger log = LoggerFactory.getLogger(PurgeExpiredTokensJob.class);

	private final RefreshTokenRepository refreshTokens;

	PurgeExpiredTokensJob(RefreshTokenRepository refreshTokens) {
		this.refreshTokens = refreshTokens;
	}

	@Scheduled(cron = "${app.jwt.purge-cron:0 0 3 * * *}")
	@Transactional
	void purge() {
		int deleted = refreshTokens.deleteExpiredBefore(Instant.now());
		if (deleted > 0) {
			log.info("Deleted {} expired refresh tokens", deleted);
		}
	}
}
