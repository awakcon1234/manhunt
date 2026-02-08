package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class LobbyItemGuardListener implements Listener {
	private final LobbyItemService lobbyItemService;

	public LobbyItemGuardListener(LobbyItemService lobbyItemService) {
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		ItemStack current = event.getCurrentItem();
		ItemStack cursor = event.getCursor();
		if (lobbyItemService.isLobbyItem(current) || lobbyItemService.isLobbyItem(cursor)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onInventoryDrag(InventoryDragEvent event) {
		ItemStack cursor = event.getOldCursor();
		if (lobbyItemService.isLobbyItem(cursor)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDropItem(PlayerDropItemEvent event) {
		ItemStack item = event.getItemDrop().getItemStack();
		if (lobbyItemService.isLobbyItem(item)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onSwapHands(PlayerSwapHandItemsEvent event) {
		if (lobbyItemService.isLobbyItem(event.getMainHandItem())
				|| lobbyItemService.isLobbyItem(event.getOffHandItem())) {
			event.setCancelled(true);
		}
	}
}
