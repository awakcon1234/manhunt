package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.LunaManhuntPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

import java.util.Objects;

public class PlaceholderApiHookListener implements Listener {
	private final LunaManhuntPlugin plugin;

	public PlaceholderApiHookListener(LunaManhuntPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	@EventHandler
	public void onPluginEnable(PluginEnableEvent event) {
		if (event == null || event.getPlugin() == null) {
			return;
		}
		if (!"PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
			return;
		}
		plugin.onPlaceholderApiEnabled();
	}

	@EventHandler
	public void onPluginDisable(PluginDisableEvent event) {
		if (event == null || event.getPlugin() == null) {
			return;
		}
		if (!"PlaceholderAPI".equalsIgnoreCase(event.getPlugin().getName())) {
			return;
		}
		plugin.onPlaceholderApiDisabled();
	}
}
