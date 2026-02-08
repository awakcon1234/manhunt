package dev.belikhun.lunamanhunt.service.lobby;

import dev.belikhun.lunamanhunt.model.match.Match;

public class LobbyActionResult {
	private final boolean success;
	private final String message;
	private final Match match;

	private LobbyActionResult(boolean success, String message, Match match) {
		this.success = success;
		this.message = message;
		this.match = match;
	}

	public static LobbyActionResult ok(String message, Match match) {
		return new LobbyActionResult(true, message, match);
	}

	public static LobbyActionResult fail(String message) {
		return new LobbyActionResult(false, message, null);
	}

	public boolean isSuccess() {
		return success;
	}

	public String getMessage() {
		return message;
	}

	public Match getMatch() {
		return match;
	}
}
