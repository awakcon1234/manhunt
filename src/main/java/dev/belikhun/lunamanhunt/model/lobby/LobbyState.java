package dev.belikhun.lunamanhunt.model.lobby;

import java.util.ArrayList;
import java.util.List;

public class LobbyState {
	private final List<String> activeMatchIds = new ArrayList<>();
	private long lastSavedAtMillis;

	public List<String> getActiveMatchIds() {
		return activeMatchIds;
	}

	public long getLastSavedAtMillis() {
		return lastSavedAtMillis;
	}

	public void setLastSavedAtMillis(long lastSavedAtMillis) {
		this.lastSavedAtMillis = lastSavedAtMillis;
	}
}
