package dev.belikhun.lunamanhunt.util;

public final class Debug {
	private static volatile boolean enabled = false;

	private Debug() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean enabled) {
		Debug.enabled = enabled;
	}
}
