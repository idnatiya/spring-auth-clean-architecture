package com.spring.example.auth.application;

final class LogFields {

	private LogFields() {
	}

	/**
	 * First 8 characters of a token hash. Enough to correlate log lines with each other, useless
	 * to anyone who reads the logs: the raw token never appears.
	 */
	static String hashPrefix(String tokenHash) {
		return tokenHash.length() <= 8 ? tokenHash : tokenHash.substring(0, 8);
	}
}
