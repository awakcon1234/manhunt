package dev.belikhun.lunamanhunt.util;

import dev.belikhun.lunamanhunt.model.match.MatchWinner;

public final class MatchHistoryFormatter {
	private MatchHistoryFormatter() {
	}

	public static String formatOutcome(boolean won) {
		return won ? "<green>Thắng</green>" : "<red>Thua</red>";
	}

	public static String formatOutcomeWithIcon(boolean won) {
		return won ? "<green>✔ Thắng</green>" : "<red>❌ Thua</red>";
	}

	public static String formatWinner(MatchWinner winner, String fallback) {
		if (winner == null) {
			return fallback;
		}
		return switch (winner) {
			case SPEEDRUNNER -> UiColors.roleSpeedrunner();
			case HUNTERS -> UiColors.roleHunter();
		};
	}

	public static String formatDuration(long millis) {
		return TimeFormat.formatFlexibleDuration(millis, "Chưa có");
	}
}