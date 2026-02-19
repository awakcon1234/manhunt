package dev.belikhun.lunamanhunt.util;

import java.util.Locale;

public final class UiColors {
	public static final String TITLE_PRIMARY = "#1f2a3a";
	public static final String TITLE_SECONDARY = "#3a4756";
	public static final String TITLE_DANGER = "#3a1f1f";
	public static final String COMMAND_HIGHLIGHT = "#59c3ff";
	public static final String ROLE_SPEEDRUNNER = "#38F8FF";
	public static final String ROLE_SPEEDRUNNER_ACCENT = "#8B5CFF";
	public static final String ROLE_HUNTER = "#FFB347";
	public static final String ROLE_HUNTER_ACCENT = "#FF4D6D";
	private static final String DEFAULT_ROOM_ID_GRADIENT = "#7ED6FF:#A78BFA";

	private UiColors() {
	}

	public static String titlePrimary(String text) {
		return wrap(TITLE_PRIMARY, text);
	}

	public static String titleSecondary(String text) {
		return wrap(TITLE_SECONDARY, text);
	}

	public static String titleDanger(String text) {
		return wrap(TITLE_DANGER, text);
	}

	public static String command(String command) {
		if (command == null || command.isBlank()) {
			return "";
		}
		String display = command.replace("<", "\\<").replace(">", "\\>");
		String styled = wrap(COMMAND_HIGHLIGHT, display);
		return "<click:suggest_command:'" + command + "'>"
			+ "<hover:show_text:'<gray>Bấm để dán lệnh </gray>" + styled + "'>"
			+ styled
			+ "</hover></click>";
	}

	public static String speedrunner(String text) {
		return wrapGradient(ROLE_SPEEDRUNNER, ROLE_SPEEDRUNNER_ACCENT, text);
	}

	public static String roleSpeedrunner() {
		return speedrunner("Speedrunner");
	}

	public static String roleSpeedrunnerBold() {
		return speedrunner("<b>Speedrunner</b>");
	}

	public static String hunter(String text) {
		return wrapGradient(ROLE_HUNTER, ROLE_HUNTER_ACCENT, text);
	}

	public static String roleHunter() {
		return hunter("Thợ săn");
	}

	public static String roleHunterLower() {
		return hunter("thợ săn");
	}

	public static String roleHunterBold() {
		return hunter("<b>Thợ săn</b>");
	}

	public static String roleHuntersBold() {
		return hunter("<b>Thợ săn</b>");
	}

	public static String matchIdText(String matchId) {
		String safeId = safeMatchId(matchId);
		String roomGradient = resolveRoomIdGradient(safeId);
		return "<gradient:" + roomGradient + ">#" + safeId + "</gradient>";
	}

	public static String matchIdChip(String matchId) {
		return "<white>⌂</white> " + matchIdText(matchId);
	}

	public static String wrap(String hex, String text) {
		if (hex == null || hex.isBlank()) {
			return text;
		}
		return "<color:" + hex + ">" + text + "</color>";
	}

	public static String wrapGradient(String startHex, String endHex, String text) {
		if (startHex == null || startHex.isBlank() || endHex == null || endHex.isBlank()) {
			return text;
		}
		return "<gradient:" + startHex + ":" + endHex + ">" + text + "</gradient>";
	}

	private static String safeMatchId(String matchId) {
		if (matchId == null || matchId.isBlank()) {
			return "-";
		}
		return matchId.trim();
	}

	private static String resolveRoomIdGradient(String matchId) {
		if (matchId == null || matchId.isBlank() || "-".equals(matchId)) {
			return DEFAULT_ROOM_ID_GRADIENT;
		}
		String normalizedId = matchId.toUpperCase(Locale.ROOT);
		int idHash = normalizedId.hashCode();

		int startHue = Math.floorMod(idHash, 360);
		int hueOffset = 30 + Math.floorMod(idHash >>> 8, 90);
		int endHue = Math.floorMod(startHue + hueOffset, 360);

		int startSaturation = 78 + Math.floorMod(idHash >>> 4, 18);
		int endSaturation = 78 + Math.floorMod(idHash >>> 12, 18);
		int startLightness = 58 + Math.floorMod(idHash >>> 20, 12);
		int endLightness = 54 + Math.floorMod(idHash >>> 24, 12);

		String startHex = hslToHex(startHue, startSaturation, startLightness);
		String endHex = hslToHex(endHue, endSaturation, endLightness);
		return startHex + ":" + endHex;
	}

	private static String hslToHex(int hueDegrees, int saturationPercent, int lightnessPercent) {
		double hue = Math.floorMod(hueDegrees, 360) / 360.0;
		double saturation = clampUnit(saturationPercent / 100.0);
		double lightness = clampUnit(lightnessPercent / 100.0);

		double red;
		double green;
		double blue;
		if (saturation == 0.0) {
			red = lightness;
			green = lightness;
			blue = lightness;
		} else {
			double q = lightness < 0.5
				? lightness * (1.0 + saturation)
				: lightness + saturation - (lightness * saturation);
			double p = (2.0 * lightness) - q;
			red = hueToRgb(p, q, hue + (1.0 / 3.0));
			green = hueToRgb(p, q, hue);
			blue = hueToRgb(p, q, hue - (1.0 / 3.0));
		}

		int redChannel = clampColorChannel((int) Math.round(red * 255.0));
		int greenChannel = clampColorChannel((int) Math.round(green * 255.0));
		int blueChannel = clampColorChannel((int) Math.round(blue * 255.0));
		return String.format(Locale.US, "#%02X%02X%02X", redChannel, greenChannel, blueChannel);
	}

	private static double hueToRgb(double p, double q, double t) {
		double adjusted = t;
		if (adjusted < 0.0) {
			adjusted += 1.0;
		}
		if (adjusted > 1.0) {
			adjusted -= 1.0;
		}
		if (adjusted < (1.0 / 6.0)) {
			return p + ((q - p) * 6.0 * adjusted);
		}
		if (adjusted < 0.5) {
			return q;
		}
		if (adjusted < (2.0 / 3.0)) {
			return p + ((q - p) * ((2.0 / 3.0) - adjusted) * 6.0);
		}
		return p;
	}

	private static double clampUnit(double value) {
		return Math.max(0.0, Math.min(1.0, value));
	}

	private static int clampColorChannel(int value) {
		return Math.max(0, Math.min(255, value));
	}
}
