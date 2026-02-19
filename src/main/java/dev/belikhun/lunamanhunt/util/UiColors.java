package dev.belikhun.lunamanhunt.util;

public final class UiColors {
	public static final String TITLE_PRIMARY = "#1f2a3a";
	public static final String TITLE_SECONDARY = "#3a4756";
	public static final String TITLE_DANGER = "#3a1f1f";
	public static final String COMMAND_HIGHLIGHT = "#59c3ff";
	public static final String ROLE_SPEEDRUNNER = "#38F8FF";
	public static final String ROLE_SPEEDRUNNER_ACCENT = "#8B5CFF";
	public static final String ROLE_HUNTER = "#FFB347";
	public static final String ROLE_HUNTER_ACCENT = "#FF4D6D";

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
}
