package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.integration.NetherPortalsService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.configuration.file.YamlConfiguration;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MatchEndService {
	private static final int WORLD_DELETE_RETRY_LIMIT = 5;
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MultiverseService multiverseService;
	private final NetherPortalsService netherPortalsService;
	private final LobbyTeleportService lobbyTeleportService;
	private final CardService cardService;
	private final File cleanupFile;
	private final Map<String, UUID> awaitingPortal = new ConcurrentHashMap<>();
	private final Map<String, Integer> deleteWorldRetries = new ConcurrentHashMap<>();

	public MatchEndService(JavaPlugin plugin, LobbyManager lobbyManager, MultiverseService multiverseService,
			NetherPortalsService netherPortalsService, LobbyTeleportService lobbyTeleportService,
			CardService cardService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.multiverseService = Objects.requireNonNull(multiverseService, "multiverseService");
		this.netherPortalsService = Objects.requireNonNull(netherPortalsService, "netherPortalsService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.cleanupFile = new File(plugin.getDataFolder(), "cleanup-worlds.yml");
	}

	public void processQueuedWorlds() {
		for (String worldName : loadQueuedWorlds()) {
			deleteWorld(worldName);
		}
	}

	public void endWithHunters(Match match, String message, Location deathLocation, boolean showSequence) {
		if (match == null) {
			return;
		}
		match.setWinner(MatchWinner.HUNTERS);
		match.setState(MatchState.ENDING);
		broadcast(match, message);
		if (!showSequence) {
			finishMatch(match, true);
			return;
		}
		startHunterWinSequence(match, deathLocation);
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
		finishMatch(match, false);
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
		match.getKeptCards().clear();
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

	private void startHunterWinSequence(Match match, Location deathLocation) {
		if (match == null) {
			return;
		}
		Location center = resolveHunterWinLocation(match, deathLocation);
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) {
				continue;
			}
			player.setGameMode(GameMode.SPECTATOR);
			if (center != null) {
				player.teleport(center);
			}
		}
		startHunterFireworks(match, center);
		new BukkitRunnable() {
			@Override
			public void run() {
				finishMatch(match, true);
			}
		}.runTaskLater(plugin, 20L * 10);
	}

	private Location resolveHunterWinLocation(Match match, Location deathLocation) {
		if (deathLocation != null) {
			return deathLocation.clone();
		}
		MatchWorlds worlds = match.getWorlds();
		World overworld = worlds == null ? null : Bukkit.getWorld(worlds.getOverworldName());
		return overworld == null ? null : overworld.getSpawnLocation();
	}

	private void startHunterFireworks(Match match, Location center) {
		if (match == null || center == null) {
			return;
		}
		World world = center.getWorld();
		if (world == null) {
			return;
		}
		new BukkitRunnable() {
			private int ticks = 0;

			@Override
			public void run() {
				if (ticks >= 200) {
					cancel();
					return;
				}
				for (int i = 0; i < 4; i++) {
					spawnFirework(world, center, 10);
				}
				ticks += 10;
			}
		}.runTaskTimer(plugin, 0L, 10L);
	}

	private void finishMatch(Match match, boolean clearInventory) {
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
				if (clearInventory) {
					player.getInventory().clear();
					player.getInventory().setArmorContents(null);
					player.getInventory().setItemInOffHand(null);
					player.getInventory().setExtraContents(null);
				}
				lobbyTeleportService.teleportToLobby(player, false);
			}
		}

		cleanupWorlds(match);
		match.getHunters().clear();
		match.getDrawnCards().clear();
		match.getKeptCards().clear();
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
		netherPortalsService.unlinkMatchWorlds(
			worlds.getOverworldName(),
			worlds.getNetherName(),
			worlds.getEndName()
		);
		deleteWorld(worlds.getOverworldName());
		deleteWorld(worlds.getNetherName());
		deleteWorld(worlds.getEndName());
	}

	private void deleteWorld(String worldName) {
		if (worldName == null || worldName.isBlank()) {
			return;
		}
		if (Bukkit.getServer().isStopping()) {
			queueWorldCleanup(worldName);
			return;
		}
		World world = Bukkit.getWorld(worldName);
		if (world != null && !world.getPlayers().isEmpty()) {
			int retries = deleteWorldRetries.getOrDefault(worldName, 0);
			if (retries >= WORLD_DELETE_RETRY_LIMIT) {
				deleteWorldRetries.remove(worldName);
				queueWorldCleanup(worldName);
				return;
			}
			deleteWorldRetries.put(worldName, retries + 1);
			Bukkit.getScheduler().runTaskLater(plugin, () -> deleteWorld(worldName), 40L);
			return;
		}
		if (world != null) {
			Bukkit.unloadWorld(world, false);
		}
		deleteWorldRetries.remove(worldName);
		removeWorldFromMultiverse(worldName);
		File folder = new File(Bukkit.getWorldContainer(), worldName);
		if (deleteFolder(folder)) {
			removeQueuedWorld(worldName);
		} else {
			queueWorldCleanup(worldName);
		}
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

	private boolean deleteFolder(File folder) {
		if (folder == null || !folder.exists()) {
			return true;
		}
		File[] files = folder.listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					if (!deleteFolder(file)) {
						return false;
					}
				} else {
					if (!file.delete()) {
						return false;
					}
				}
			}
		}
		return folder.delete();
	}

	private void queueWorldCleanup(String worldName) {
		if (worldName == null || worldName.isBlank()) {
			return;
		}
		List<String> queued = loadQueuedWorlds();
		if (!queued.contains(worldName)) {
			queued.add(worldName);
			saveQueuedWorlds(queued);
		}
	}

	private void removeQueuedWorld(String worldName) {
		List<String> queued = loadQueuedWorlds();
		if (queued.remove(worldName)) {
			saveQueuedWorlds(queued);
		}
	}

	private List<String> loadQueuedWorlds() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(cleanupFile);
		return new ArrayList<>(config.getStringList("worlds"));
	}

	private void saveQueuedWorlds(List<String> worlds) {
		YamlConfiguration config = new YamlConfiguration();
		config.set("worlds", worlds);
		try {
			config.save(cleanupFile);
		} catch (IOException ignored) {
		}
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
