package dev.belikhun.lunamanhunt.util;

public final class UiColors {
	public static final String TITLE_PRIMARY = "#1f2a3a";
	public static final String TITLE_SECONDARY = "#3a4756";
	public static final String TITLE_DANGER = "#3a1f1f";
	public static final String COMMAND_HIGHLIGHT = "#59c3ff";
	public static final String ROLE_SPEEDRUNNER = "#8fd3ff";
	public static final String ROLE_HUNTER = "#ffb84d";

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
		return wrap(ROLE_SPEEDRUNNER, text);
	}

	public static String hunter(String text) {
		return wrap(ROLE_HUNTER, text);
	}

	public static String wrap(String hex, String text) {
		if (hex == null || hex.isBlank()) {
			return text;
		}
		return "<color:" + hex + ">" + text + "</color>";
	}
}
