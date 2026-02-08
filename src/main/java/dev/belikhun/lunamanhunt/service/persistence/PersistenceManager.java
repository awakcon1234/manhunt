package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class PersistenceManager {
	private final ProfileStore profileStore;
	private final MatchTemplateStore matchTemplateStore;
	private final LobbyStateStore lobbyStateStore;

	public PersistenceManager(JavaPlugin plugin, MatchSettingsService matchSettingsService) {
		File dataFolder = plugin.getDataFolder();
		if (!dataFolder.exists() && !dataFolder.mkdirs()) {
			throw new IllegalStateException("Unable to create plugin data folder");
		}

		this.profileStore = new ProfileStore(new File(dataFolder, "profiles"));
		this.matchTemplateStore = new MatchTemplateStore(new File(dataFolder, "match-templates.yml"),
			matchSettingsService);
		this.lobbyStateStore = new LobbyStateStore(new File(dataFolder, "lobby-state.yml"));
	}

	public ProfileStore getProfileStore() {
		return profileStore;
	}

	public MatchTemplateStore getMatchTemplateStore() {
		return matchTemplateStore;
	}

	public LobbyStateStore getLobbyStateStore() {
		return lobbyStateStore;
	}
}
