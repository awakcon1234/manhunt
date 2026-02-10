package dev.belikhun.lunamanhunt.model.match;

public class MatchTemplate {
	private final String name;
	private MatchSettings settings;

	public MatchTemplate(String name, MatchSettings settings) {
		this.name = name;
		this.settings = settings;
	}

	public String getName() {
		return name;
	}

	public MatchSettings getSettings() {
		return settings;
	}

	public void setSettings(MatchSettings settings) {
		this.settings = settings;
	}
}
