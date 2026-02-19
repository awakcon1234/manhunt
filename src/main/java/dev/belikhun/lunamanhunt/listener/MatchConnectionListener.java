package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.MatchDisconnectService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public class MatchConnectionListener implements Listener {
	private final MatchDisconnectService disconnectService;

	public MatchConnectionListener(MatchDisconnectService disconnectService) {
		this.disconnectService = Objects.requireNonNull(disconnectService, "disconnectService");
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		disconnectService.handlePlayerQuit(player);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (event == null) {
			return;
		}
		disconnectService.handlePlayerJoin(event.getPlayer());
	}
}
