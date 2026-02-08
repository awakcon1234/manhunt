package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public class LobbyStateListener implements Listener {
	private final LobbyItemService lobbyItemService;
	private final LobbyTeleportService lobbyTeleportService;

	public LobbyStateListener(LobbyItemService lobbyItemService, LobbyTeleportService lobbyTeleportService) {
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		lobbyTeleportService.teleportToLobby(event.getPlayer(), false);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		lobbyItemService.applyLobbyStateIfEligible(event.getPlayer());
	}
}
