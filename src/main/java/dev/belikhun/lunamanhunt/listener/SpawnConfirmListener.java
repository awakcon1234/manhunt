package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.spawn.SpawnService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class SpawnConfirmListener implements Listener {
	private final SpawnService spawnService;

	public SpawnConfirmListener(SpawnService spawnService) {
		this.spawnService = Objects.requireNonNull(spawnService, "spawnService");
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		Inventory inventory = event.getInventory();
		if (!spawnService.isConfirmInventory(inventory)) {
			return;
		}
		event.setCancelled(true);
		ItemStack item = event.getCurrentItem();
		String matchId = spawnService.getMatchId(inventory);
		if (matchId == null) {
			return;
		}
		spawnService.handleMenuClick(player, item, matchId);
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player)) {
			return;
		}
		Inventory inventory = event.getInventory();
		if (!spawnService.isConfirmInventory(inventory)) {
			return;
		}
		spawnService.handleMenuClose(player);
	}
}
