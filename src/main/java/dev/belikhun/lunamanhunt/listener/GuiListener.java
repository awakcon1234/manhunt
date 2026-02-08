package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.gui.GuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class GuiListener implements Listener {
	private final GuiService guiService;

	public GuiListener(GuiService guiService) {
		this.guiService = Objects.requireNonNull(guiService, "guiService");
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) {
			return;
		}
		Inventory inventory = event.getInventory();
		if (!guiService.isGuiInventory(inventory)) {
			return;
		}
		if (guiService.isKitEditorInventory(inventory)) {
			return;
		}
		event.setCancelled(true);
		ItemStack item = event.getCurrentItem();
		guiService.handleMenuClick(player, inventory, item);
	}
}
