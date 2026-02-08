package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchEndService {
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MultiverseService multiverseService;
	private final LobbyTeleportService lobbyTeleportService;
	private final CardService cardService;
	private final Map<String, UUID> awaitingPortal = new ConcurrentHashMap<>();

	public MatchEndService(JavaPlugin plugin, LobbyManager lobbyManager, MultiverseService multiverseService,
			LobbyTeleportService lobbyTeleportService, CardService cardService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.multiverseService = Objects.requireNonNull(multiverseService, "multiverseService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
	}

	public void endWithHunters(Match match, String message) {
		if (match == null) {
			return;
		}
		match.setWinner(MatchWinner.HUNTERS);
		broadcast(match, message);
		finishMatch(match);
	}

	public void endWithSpeedrunner(Match match, String message) {
		if (match == null) {
			return;
		}
		match.setWinner(MatchWinner.SPEEDRUNNER);
		match.setState(MatchState.ENDING);
		broadcast(match, message);
		broadcast(match, "<yellow>ℹ</yellow> Speedrunner hãy vào cổng End để kết thúc trận đấu.");

		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null) {
			awaitingPortal.put(match.getId(), speedrunnerId);
		}

		teleportHuntersToEnd(match);
		startEndFireworks(match);
	}

	public void handleSpeedrunnerPortal(Player player) {
		if (player == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getWinner() != MatchWinner.SPEEDRUNNER) {
			return;
		}
		UUID expected = awaitingPortal.get(match.getId());
		if (expected == null || !expected.equals(player.getUniqueId())) {
			return;
		}
		finishMatch(match);
	}

	public void forceCleanup(Match match) {
		if (match == null) {
			return;
		}
		awaitingPortal.remove(match.getId());
		cardService.clearActiveCards(match);
		match.setState(MatchState.CLEANUP);
		match.setEndedAtMillis(System.currentTimeMillis());
		cleanupWorlds(match);
		match.getHunters().clear();
		match.getDrawnCards().clear();
		match.setSpeedrunnerPoints(0);
		match.setSpeedrunnerId(null);
		match.setState(MatchState.FINISHED);
		lobbyManager.disbandMatch(match);
	}

	private void teleportHuntersToEnd(Match match) {
		MatchWorlds worlds = match.getWorlds();
		World endWorld = worlds == null ? null : Bukkit.getWorld(worlds.getEndName());
		Location target = endWorld == null ? null : endWorld.getSpawnLocation();

		for (UUID hunterId : Set.copyOf(match.getHunters())) {
			Player hunter = Bukkit.getPlayer(hunterId);
			if (hunter == null) {
				continue;
			}
			hunter.setGameMode(GameMode.SPECTATOR);
			if (target != null) {
				hunter.teleport(target);
			}
		}
	}

	private void startEndFireworks(Match match) {
		MatchWorlds worlds = match.getWorlds();
		World endWorld = worlds == null ? null : Bukkit.getWorld(worlds.getEndName());
		if (endWorld == null) {
			return;
		}
		Location center = endWorld.getSpawnLocation();
		new BukkitRunnable() {
			private int cycles = 0;

			@Override
			public void run() {
				if (cycles >= 6) {
					cancel();
					return;
				}
				for (int i = 0; i < 6; i++) {
					spawnFirework(endWorld, center, 12);
				}
				cycles++;
			}
		}.runTaskTimer(plugin, 0L, 20L * 2);
	}

	private void spawnFirework(World world, Location center, int radius) {
		int offsetX = (int) (Math.random() * radius * 2) - radius;
		int offsetZ = (int) (Math.random() * radius * 2) - radius;
		Location loc = center.clone().add(offsetX, 2, offsetZ);
		Firework firework = world.spawn(loc, Firework.class);
		FireworkMeta meta = firework.getFireworkMeta();
		meta.addEffect(org.bukkit.FireworkEffect.builder()
			.withColor(Color.fromRGB(255, 180, 60))
			.withFade(Color.fromRGB(120, 220, 255))
			.with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
			.flicker(true)
			.trail(true)
			.build());
		meta.setPower(1);
		firework.setFireworkMeta(meta);
	}

	private void finishMatch(Match match) {
		awaitingPortal.remove(match.getId());
		cardService.clearActiveCards(match);
		match.setState(MatchState.CLEANUP);
		match.setEndedAtMillis(System.currentTimeMillis());

		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				if (player.getGameMode() == GameMode.SPECTATOR) {
					player.setGameMode(GameMode.SURVIVAL);
				}
				lobbyTeleportService.teleportToLobby(player, false);
			}
		}

		cleanupWorlds(match);
		match.getHunters().clear();
		match.getDrawnCards().clear();
		match.setSpeedrunnerPoints(0);
		match.setSpeedrunnerId(null);
		match.setState(MatchState.FINISHED);
		lobbyManager.disbandMatch(match);
	}

	private void cleanupWorlds(Match match) {
		MatchWorlds worlds = match.getWorlds();
		if (worlds == null) {
			return;
		}
		deleteWorld(worlds.getOverworldName());
		deleteWorld(worlds.getNetherName());
		deleteWorld(worlds.getEndName());
	}

	private void deleteWorld(String worldName) {
		if (worldName == null || worldName.isBlank()) {
			return;
		}
		World world = Bukkit.getWorld(worldName);
		if (world != null) {
			Bukkit.unloadWorld(world, false);
		}
		File folder = new File(Bukkit.getWorldContainer(), worldName);
		deleteFolder(folder);
		removeWorldFromMultiverse(worldName);
	}

	private void removeWorldFromMultiverse(String worldName) {
		Object manager = multiverseService.getCoreApi().getWorldManager();
		invokeIfExists(manager, "removeWorld", worldName);
		invokeIfExists(manager, "deleteWorld", worldName);
	}

	private void invokeIfExists(Object manager, String methodName, String worldName) {
		try {
			var method = manager.getClass().getMethod(methodName, String.class);
			method.invoke(manager, worldName);
		} catch (Throwable ignored) {
		}
	}

	private void deleteFolder(File folder) {
		if (folder == null || !folder.exists()) {
			return;
		}
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					deleteFolder(file);
				} else {
					file.delete();
				}
			}
		}
		folder.delete();
	}

	private void broadcast(Match match, String message) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				Text.msg(player, message);
			}
		}
	}
}
