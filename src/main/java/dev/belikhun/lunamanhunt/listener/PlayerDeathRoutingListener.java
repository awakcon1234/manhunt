package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class PlayerDeathRoutingListener implements Listener {
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final LobbyTeleportService lobbyTeleportService;

	public PlayerDeathRoutingListener(JavaPlugin plugin, LobbyManager lobbyManager,
			LobbyTeleportService lobbyTeleportService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getEntity();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (!isLobbyContext(match)) {
			return;
		}

		event.setKeepInventory(true);
		event.setKeepLevel(true);
		event.setDroppedExp(0);
		event.getDrops().clear();

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player.isOnline() && player.isDead()) {
				player.spigot().respawn();
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (isLobbyContext(match)) {
			event.setRespawnLocation(lobbyTeleportService.resolveLobbySpawn(player));
			return;
		}

		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}

		Location current = event.getRespawnLocation();
		if (isMatchWorld(match, current == null ? null : current.getWorld())) {
			return;
		}

		Location deathWorldSpawn = null;
		World deathWorld = player.getWorld();
		if (isMatchWorld(match, deathWorld)) {
			deathWorldSpawn = deathWorld.getSpawnLocation();
		}
		if (deathWorldSpawn == null) {
			deathWorldSpawn = resolveOverworldSpawn(match);
		}
		if (deathWorldSpawn != null) {
			event.setRespawnLocation(deathWorldSpawn);
		}
	}

	private boolean isLobbyContext(Match match) {
		if (match == null || match.getState() == null) {
			return true;
		}
		return switch (match.getState()) {
			case LOBBY, WORLD_CREATING, WORLD_READY -> true;
			default -> false;
		};
	}

	private boolean isMatchWorld(Match match, World world) {
		if (match == null || match.getWorlds() == null || world == null) {
			return false;
		}
		String name = world.getName();
		MatchWorlds worlds = match.getWorlds();
		return name.equals(worlds.getOverworldName())
			|| name.equals(worlds.getNetherName())
			|| name.equals(worlds.getEndName());
	}

	private Location resolveOverworldSpawn(Match match) {
		if (match == null || match.getWorlds() == null) {
			return null;
		}
		World overworld = Bukkit.getWorld(match.getWorlds().getOverworldName());
		return overworld == null ? null : overworld.getSpawnLocation();
	}
}
