package com.spring.example.auth.domain;

import java.util.Locale;

public final class Usernames {

	private Usernames() {
	}

	/** So that "Andi", " andi " and "ANDI" all refer to the same account. */
	public static String normalize(String username) {
		return username == null ? null : username.trim().toLowerCase(Locale.ROOT);
	}
}
