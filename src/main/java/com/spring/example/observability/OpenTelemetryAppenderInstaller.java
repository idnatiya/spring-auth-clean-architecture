package com.spring.example.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Logback starts before the Spring context, so the appender declared in logback-spring.xml has no
 * OpenTelemetry instance yet. Hand it one as soon as the context is up, otherwise it silently drops
 * every log record.
 */
@Component
class OpenTelemetryAppenderInstaller implements InitializingBean {

	private final OpenTelemetry openTelemetry;

	OpenTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
		this.openTelemetry = openTelemetry;
	}

	@Override
	public void afterPropertiesSet() {
		OpenTelemetryAppender.install(this.openTelemetry);
	}
}
