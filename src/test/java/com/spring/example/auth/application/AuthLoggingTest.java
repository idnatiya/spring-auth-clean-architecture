package com.spring.example.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.spring.example.auth.domain.AuthTokens;
import com.spring.example.auth.domain.InvalidCredentialsException;
import com.spring.example.auth.domain.InvalidRefreshTokenException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthLoggingTest {

	private static final String PASSWORD = "rahasia123";

	@Autowired
	private RegisterUserUseCase registerUser;

	@Autowired
	private LoginUseCase login;

	@Autowired
	private RefreshTokenUseCase refreshToken;

	private ListAppender<ILoggingEvent> appender;

	private ch.qos.logback.classic.Logger authLogger;

	@BeforeEach
	void captureAuthLogs() {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		authLogger = context.getLogger("com.spring.example.auth");
		authLogger.setLevel(Level.DEBUG);
		appender = new ListAppender<>();
		appender.setContext(context);
		appender.start();
		authLogger.addAppender(appender);
	}

	@AfterEach
	void releaseAppender() {
		authLogger.detachAppender(appender);
		appender.stop();
		MDC.clear();
	}

	@Test
	void reuseDetectionLogsWarnWithTokenFamily() {
		AuthTokens tokens = registerAndLogin("reuselog-" + System.nanoTime());
		AuthTokens rotated = refreshToken.execute(tokens.refreshToken());

		assertThatThrownBy(() -> refreshToken.execute(tokens.refreshToken()))
			.isInstanceOf(InvalidRefreshTokenException.class);

		ILoggingEvent reuse = eventsWithMessage("reuse detected").get(0);
		assertThat(reuse.getLevel()).isEqualTo(Level.WARN);
		Map<String, String> mdc = reuse.getMDCPropertyMap();
		assertThat(mdc).containsKeys("username", "token_family", "token_hash_prefix");
		// The prefix is a fingerprint, never something replayable.
		assertThat(mdc.get("token_hash_prefix")).hasSize(8);
		assertThat(rotated.refreshToken()).isNotEqualTo(tokens.refreshToken());
	}

	@Test
	void mdcIsClearedAfterSuccessAndAfterFailure() {
		String username = "mdclog-" + System.nanoTime();
		registerAndLogin(username);
		assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

		assertThatThrownBy(() -> login.execute(username, "wrongpassword"))
			.isInstanceOf(InvalidCredentialsException.class);
		// A leaked entry here would tag the next request on this thread with the wrong username.
		assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
	}

	@Test
	void secretsNeverReachTheLogs() {
		AuthTokens tokens = registerAndLogin("secretlog-" + System.nanoTime());
		refreshToken.execute(tokens.refreshToken());

		String everything = appender.list.stream()
			.map(event -> event.getFormattedMessage() + " " + event.getMDCPropertyMap())
			.reduce("", (a, b) -> a + "\n" + b);

		assertThat(everything).doesNotContain(PASSWORD);
		assertThat(everything).doesNotContain(tokens.refreshToken());
		assertThat(everything).doesNotContain(tokens.access().value());
	}

	private AuthTokens registerAndLogin(String username) {
		registerUser.execute(username, PASSWORD, "127.0.0.1");
		return login.execute(username, PASSWORD);
	}

	private List<ILoggingEvent> eventsWithMessage(String fragment) {
		List<ILoggingEvent> found = appender.list.stream()
			.filter(event -> event.getFormattedMessage().contains(fragment))
			.toList();
		assertThat(found).as("log events containing '%s'", fragment).isNotEmpty();
		return found;
	}
}
