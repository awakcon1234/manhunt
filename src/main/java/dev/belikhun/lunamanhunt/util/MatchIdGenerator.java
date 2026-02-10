package dev.belikhun.lunamanhunt.util;

import java.security.SecureRandom;

public final class MatchIdGenerator {
	private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final SecureRandom RANDOM = new SecureRandom();

	private MatchIdGenerator() {
	}

	public static String generateId(int length) {
		if (length <= 0) {
			throw new IllegalArgumentException("length must be positive");
		}

		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			int index = RANDOM.nextInt(ALPHABET.length());
			builder.append(ALPHABET.charAt(index));
		}

		return builder.toString();
	}
}
