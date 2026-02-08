package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.gui.GuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Objects;

public class GuiInputListener implements Listener {
	private final JavaPlugin plugin;
	private final GuiService guiService;

	public GuiInputListener(JavaPlugin plugin, GuiService guiService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.guiService = Objects.requireNonNull(guiService, "guiService");
	}

	@EventHandler
	public void onChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		if (!guiService.hasSeedInput(player)) {
			return;
		}
		String message = PlainTextComponentSerializer.plainText().serialize(event.message());
		event.setCancelled(true);
		Bukkit.getScheduler().runTask(plugin, () -> guiService.handleSeedInput(player, message));
	}

	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player player)) {
			return;
		}
		Inventory inventory = event.getInventory();
		if (!guiService.isKitEditorInventory(inventory)) {
			return;
		}
		guiService.handleKitClose(player, inventory);
	}
}
