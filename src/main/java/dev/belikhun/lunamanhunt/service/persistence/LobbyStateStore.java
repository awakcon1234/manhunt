package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.model.lobby.LobbyState;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class LobbyStateStore {
	private final File file;

	public LobbyStateStore(File file) {
		this.file = file;
	}

	public LobbyState load() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		LobbyState state = new LobbyState();

		List<String> activeMatchIds = config.getStringList("activeMatchIds");
		if (!activeMatchIds.isEmpty()) {
			state.getActiveMatchIds().addAll(activeMatchIds);
		}

		state.setLastSavedAtMillis(config.getLong("lastSavedAtMillis", 0L));
		return state;
	}

	public void save(LobbyState state) throws IOException {
		YamlConfiguration config = new YamlConfiguration();
		config.set("activeMatchIds", state.getActiveMatchIds());
		config.set("lastSavedAtMillis", state.getLastSavedAtMillis());
		config.save(file);
	}
}
