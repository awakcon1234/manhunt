package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class LobbyHotbarListener implements Listener {
	private final LobbyItemService lobbyItemService;

	public LobbyHotbarListener(LobbyItemService lobbyItemService) {
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event == null) {
			return;
		}
		ItemStack item = event.getItem();
		if (item == null) {
			return;
		}
		Player player = event.getPlayer();
		if (!lobbyItemService.isLobbyWorld(player.getWorld())) {
			return;
		}
		if (lobbyItemService.handleInteract(player, item)) {
			event.setCancelled(true);
		}
	}
}
