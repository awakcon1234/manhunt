package dev.belikhun.lunamanhunt.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class TimeFormat {
	private static final DateTimeFormatter DEFAULT_TIMESTAMP_FORMATTER = DateTimeFormatter
		.ofPattern("dd/MM/yyyy HH:mm")
		.withZone(ZoneId.systemDefault());

	private TimeFormat() {
	}

	public static String formatRunTimer(long millis) {
		long safeMillis = Math.max(0L, millis);
		long totalSeconds = safeMillis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		long ms = safeMillis % 1000L;
		return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, ms);
	}

	public static String formatFlexibleDuration(long millis, String fallback) {
		if (millis <= 0L) {
			return fallback;
		}
		long totalSeconds = millis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0) {
			return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format(Locale.US, "%d:%02d", minutes, seconds);
	}

	public static String formatMinutesSeconds(long millis, String fallback) {
		if (millis <= 0L) {
			return fallback;
		}
		long totalSeconds = millis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return String.format(Locale.US, "%02d:%02d", minutes, seconds);
	}

	public static String formatMinutesSecondsFromSeconds(long seconds) {
		long safeSeconds = Math.max(0L, seconds);
		long minutes = safeSeconds / 60L;
		long remainingSeconds = safeSeconds % 60L;
		return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds);
	}

	public static String formatHoursMinutes(long millis) {
		long safeMillis = Math.max(0L, millis);
		long totalSeconds = safeMillis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		return String.format(Locale.US, "%dh %02dm", hours, minutes);
	}

	public static String formatHoursMinutesSecondsMillis(long millis, String fallback) {
		if (millis < 0L) {
			return fallback;
		}
		long safeMillis = Math.max(0L, millis);
		long totalSeconds = safeMillis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		long ms = safeMillis % 1000L;
		return String.format(Locale.US, "%02d:%02d:%02d.%03d", hours, minutes, seconds, ms);
	}

	public static String formatEtaSeconds(long seconds, String fallback) {
		if (seconds <= 0L) {
			return fallback;
		}
		long safeSeconds = Math.max(0L, seconds);
		long minutes = safeSeconds / 60L;
		long remaining = safeSeconds % 60L;
		return String.format(Locale.US, "%02d:%02ds", minutes, remaining);
	}

	public static String formatRelativeCompact(long nowMillis, long timeMillis) {
		long delta = Math.max(0L, nowMillis - Math.max(0L, timeMillis));
		long minutes = delta / 60000L;
		if (minutes < 1) {
			return "vừa xong";
		}
		if (minutes < 60) {
			return minutes + "p trước";
		}
		long hours = minutes / 60L;
		if (hours < 24) {
			return hours + "h trước";
		}
		long days = hours / 24L;
		return days + "d trước";
	}

	public static String formatRelativeDetailed(long nowMillis, long timeMillis, String fallback) {
		if (timeMillis <= 0L) {
			return fallback;
		}
		long deltaSeconds = Math.max(0L, (nowMillis - timeMillis) / 1000L);
		if (deltaSeconds < 60L) {
			return "Vừa xong";
		}
		long minutes = deltaSeconds / 60L;
		if (minutes < 60L) {
			return minutes + " phút trước";
		}
		long hours = minutes / 60L;
		if (hours < 24L) {
			return hours + " giờ trước";
		}
		long days = hours / 24L;
		if (days < 7L) {
			return days + " ngày trước";
		}
		long weeks = days / 7L;
		if (weeks < 4L) {
			return weeks + " tuần trước";
		}
		long months = days / 30L;
		if (months < 12L) {
			return months + " tháng trước";
		}
		long years = days / 365L;
		return years + " năm trước";
	}

	public static String formatTimestamp(long millis, String fallback) {
		if (millis <= 0L) {
			return fallback;
		}
		return DEFAULT_TIMESTAMP_FORMATTER.format(Instant.ofEpochMilli(millis));
	}
}