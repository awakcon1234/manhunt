package dev.belikhun.lunamanhunt.util;

import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.WorldDifficultySetting;
import dev.belikhun.lunamanhunt.model.match.WorldTimeSetting;
import dev.belikhun.lunamanhunt.model.match.WorldWeatherSetting;

import java.text.NumberFormat;
import java.util.Locale;

public final class MatchSettingsFormatter {
	private MatchSettingsFormatter() {
	}

	public static String formatVisibility(MatchVisibility visibility) {
		if (visibility == null) {
			return "Không rõ";
		}
		return switch (visibility) {
			case PUBLIC -> "Công khai";
			case INVITE_ONLY -> "Chỉ mời";
			case PRIVATE -> "Riêng tư";
		};
	}

	public static String formatVisibilityColored(MatchVisibility visibility) {
		if (visibility == null) {
			return "<gray>Không rõ</gray>";
		}
		return switch (visibility) {
			case PUBLIC -> "<#7fd36b>Công khai</#7fd36b>";
			case INVITE_ONLY -> "<#ffd166>Chỉ mời</#ffd166>";
			case PRIVATE -> "<#ff6b6b>Riêng tư</#ff6b6b>";
		};
	}

	public static String formatVisibilityColored(MatchVisibility visibility, MatchVisibility defaultVisibility) {
		if (visibility == null) {
			return "<gray>Không rõ</gray>";
		}
		if (visibility == MatchVisibility.PRIVATE) {
			return "<#ff6b6b>Riêng tư</#ff6b6b>";
		}
		String color = visibility == defaultVisibility ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatVisibility(visibility) + "</" + color + ">";
	}

	public static String formatWorldTime(WorldTimeSetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case CYCLE -> "Chu kỳ";
			case MORNING -> "Buổi sáng";
			case NOON -> "Buổi trưa";
			case NIGHT -> "Ban đêm";
			case MIDNIGHT -> "Nửa đêm";
		};
	}

	public static String formatWorldTimeColored(WorldTimeSetting setting, WorldTimeSetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldTime(setting) + "</" + color + ">";
	}

	public static String formatWorldWeather(WorldWeatherSetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case CYCLE -> "Chu kỳ";
			case CLEAR -> "Trời quang";
			case RAIN -> "Mưa";
			case THUNDERSTORM -> "Giông";
		};
	}

	public static String formatWorldWeatherColored(WorldWeatherSetting setting, WorldWeatherSetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldWeather(setting) + "</" + color + ">";
	}

	public static String formatWorldDifficulty(WorldDifficultySetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case PEACEFUL -> "Hòa bình";
			case EASY -> "Dễ";
			case MEDIUM -> "Trung bình";
			case HARD -> "Khó";
		};
	}

	public static String formatWorldDifficultyColored(WorldDifficultySetting setting,
			WorldDifficultySetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldDifficulty(setting) + "</" + color + ">";
	}

	public static String formatKeepInventory(boolean keepInventory) {
		return keepInventory ? "Bật" : "Tắt";
	}

	public static String formatKeepInventoryColored(boolean keepInventory, boolean defaultKeepInventory) {
		String color = keepInventory == defaultKeepInventory ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatKeepInventory(keepInventory) + "</" + color + ">";
	}

	public static String formatSeedValue(String seed, String defaultSeed) {
		String trimmed = seed == null ? "" : seed.trim();
		if (trimmed.isEmpty()) {
			return "<#ff6b6b>Chưa đặt</#ff6b6b>";
		}
		String defaultTrimmed = defaultSeed == null ? "" : defaultSeed.trim();
		String color = trimmed.equals(defaultTrimmed) ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + trimmed + "</" + color + ">";
	}

	public static String formatGenVersionValue(String version, String defaultVersion) {
		String trimmed = version == null ? "" : version.trim();
		if (trimmed.isEmpty()) {
			return "<#ff6b6b>Chưa đặt</#ff6b6b>";
		}
		String defaultTrimmed = defaultVersion == null ? "" : defaultVersion.trim();
		String color = trimmed.equalsIgnoreCase(defaultTrimmed) ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + trimmed + "</" + color + ">";
	}

	public static String formatNumericValue(int value, int defaultValue) {
		String color = value == defaultValue ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatNumber(value) + "</" + color + ">";
	}

	public static String formatNumber(int value) {
		NumberFormat format = NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
		return format.format(value);
	}
}