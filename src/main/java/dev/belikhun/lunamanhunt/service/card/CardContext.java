package dev.belikhun.lunamanhunt.service.card;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class CardContext {
	private final JavaPlugin plugin;
	private final Match match;
	private final Player speedrunner;
	private final LobbyManager lobbyManager;
	private final CompassService compassService;

	public CardContext(JavaPlugin plugin, Match match, Player speedrunner, LobbyManager lobbyManager, CompassService compassService) {
		this.plugin = plugin;
		this.match = match;
		this.speedrunner = speedrunner;
		this.lobbyManager = lobbyManager;
		this.compassService = compassService;
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}

	public Match getMatch() {
		return match;
	}

	public Player getSpeedrunner() {
		return speedrunner;
	}

	public LobbyManager getLobbyManager() {
		return lobbyManager;
	}

	public CompassService getCompassService() {
		return compassService;
	}

	public boolean applyPotionToSpeedrunner(PotionEffect effect) {
		if (speedrunner == null || effect == null) {
			return false;
		}
		speedrunner.addPotionEffect(effect);
		return true;
	}

	public boolean applyPotionToHunters(PotionEffect effect) {
		if (effect == null) {
			return false;
		}
		return applyToHunters(target -> target.addPotionEffect(effect));
	}

	public boolean clearPotionFromSpeedrunner(PotionEffectType type) {
		if (speedrunner == null || type == null) {
			return false;
		}
		speedrunner.removePotionEffect(type);
		return true;
	}

	public boolean clearPotionFromHunters(PotionEffectType type) {
		if (type == null) {
			return false;
		}
		return applyToHunters(target -> target.removePotionEffect(type));
	}

	public boolean applyToHunters(Consumer<Player> action) {
		if (match == null || action == null) {
			return false;
		}
		Set<UUID> targets = match.getHunters();
		if (targets.isEmpty()) {
			targets = match.getLobbyPlayers();
		}
		boolean applied = false;
		for (UUID id : targets) {
			if (speedrunner != null && id.equals(speedrunner.getUniqueId())) {
				continue;
			}
			Player target = Bukkit.getPlayer(id);
			if (target != null) {
				action.accept(target);
				applied = true;
			}
		}
		return applied;
	}

	public void giveItem(Material material, int amount) {
		if (speedrunner == null || material == null || amount <= 0) {
			return;
		}
		ItemStack item = new ItemStack(material, amount);
		speedrunner.getInventory().addItem(item);
	}

	public void runLater(Runnable task, long delayTicks) {
		if (plugin == null || task == null) {
			return;
		}
		Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
	}
}
