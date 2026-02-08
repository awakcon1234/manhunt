package dev.belikhun.lunamanhunt.util;

public final class UiColors {
	public static final String TITLE_PRIMARY = "#1f2a3a";
	public static final String TITLE_SECONDARY = "#3a4756";
	public static final String TITLE_DANGER = "#3a1f1f";

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

	public static String wrap(String hex, String text) {
		if (hex == null || hex.isBlank()) {
			return text;
		}
		return "<color:" + hex + ">" + text + "</color>";
	}
}
