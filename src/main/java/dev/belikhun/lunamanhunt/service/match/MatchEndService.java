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
import org.bukkit.Sound;
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
	private static final int SPEEDRUNNER_CLEANUP_DELAY_TICKS = 20 * 5;
	private static final int POST_TELEPORT_CLEANUP_DELAY_TICKS = 40;
	private static final int CLEANUP_RETRY_LIMIT = 5;
	private static final int CLEANUP_RETRY_DELAY_TICKS = 40;
	private static final int WORLD_DELETE_RETRY_LIMIT = 5;
	private static final int WORLD_DELETE_SPACING_TICKS = 20;
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MultiverseService multiverseService;
	private final NetherPortalsService netherPortalsService;
	private final LobbyTeleportService lobbyTeleportService;
	private final CardService cardService;
	private final File cleanupFile;
	private final Map<String, UUID> awaitingPortal = new ConcurrentHashMap<>();
	private final Map<String, Integer> deleteWorldRetries = new ConcurrentHashMap<>();
	private final Map<String, Integer> cleanupRetries = new ConcurrentHashMap<>();
	private final Map<String, Boolean> cleanupScheduled = new ConcurrentHashMap<>();

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
		scheduleEndBoards(match);
		if (!showSequence) {
			finishMatch(match, true, POST_TELEPORT_CLEANUP_DELAY_TICKS);
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
		scheduleEndBoards(match);
		broadcast(match, "<yellow>ℹ</yellow> Speedrunner hãy vào cổng End để kết thúc trận đấu.");

		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null) {
			awaitingPortal.put(match.getId(), speedrunnerId);
		}

		teleportHuntersToEnd(match);
		startEndFireworks(match);
	}

	public boolean handleSpeedrunnerPortal(Player player) {
		if (player == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getWinner() != MatchWinner.SPEEDRUNNER) {
			return false;
		}
		UUID expected = awaitingPortal.get(match.getId());
		if (expected == null || !expected.equals(player.getUniqueId())) {
			return false;
		}
		awaitingPortal.remove(match.getId());
		debugCleanup("Speedrunner portal accepted: player=" + player.getName()
			+ " match=" + match.getId()
			+ " cleanupDelayTicks=" + SPEEDRUNNER_CLEANUP_DELAY_TICKS);
		finishMatch(match, true, SPEEDRUNNER_CLEANUP_DELAY_TICKS);
		return true;
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
				finishMatch(match, true, POST_TELEPORT_CLEANUP_DELAY_TICKS);
			}
		}.runTaskLater(plugin, 20L * 10);
	}

	private void scheduleEndBoards(Match match) {
		if (match == null) {
			return;
		}
		sendBoard(match, buildWinnerBoard(match));
		new BukkitRunnable() {
			@Override
			public void run() {
				sendBoard(match, buildStatsBoard(match));
			}
		}.runTaskLater(plugin, 20L * 5);
	}

	private void sendBoard(Match match, List<String> lines) {
		if (match == null || lines == null || lines.isEmpty()) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) {
				continue;
			}
			for (String line : lines) {
				Text.tell(player, line);
			}
		}
	}

	private List<String> buildWinnerBoard(Match match) {
		String winnerLabel = match.getWinner() == MatchWinner.SPEEDRUNNER
			? "<#8fd3ff>Speedrunner</#8fd3ff>"
			: "<#ffb84d>Thợ săn</#ffb84d>";
		String speedrunnerName = resolvePlayerName(match.getSpeedrunnerId());
		String duration = formatDuration(resolveMatchDuration(match));
		int points = match.getSpeedrunnerPoints();
		int huntersKilled = match.getStats().getHuntersKilled();
		return List.of(
			"<gray>+--------------------------------------------------+</gray>",
			" <yellow>✔</yellow> <white><b>BẢNG KẾT QUẢ TRẬN ĐẤU</b></white>",
			"<gray>+--------------------------------------------------+</gray>",
			" <yellow>●</yellow> <white>Người chiến thắng: " + winnerLabel + "</white>",
			" <yellow>●</yellow> <white>Speedrunner: <#8fd3ff>" + speedrunnerName + "</#8fd3ff></white>",
			" <yellow>●</yellow> <white>Thời gian hoàn thành: <green>" + duration + "</green></white>",
			" <yellow>●</yellow> <white>Điểm thu thập: <green>" + points + "</green></white>",
			" <yellow>●</yellow> <white>Hunter bị hạ: <green>" + huntersKilled + "</green></white>",
			" <yellow>●</yellow> <white>Mã phòng: <yellow>" + match.getId() + "</yellow></white>",
			"<gray>+--------------------------------------------------+</gray>"
		);
	}

	private List<String> buildStatsBoard(Match match) {
		var stats = match.getStats();
		String netherTime = formatDuration(stats.getTimeToNetherMillis());
		String endTime = formatDuration(stats.getTimeToEndMillis());
		String damage = String.format(java.util.Locale.US, "%.1f", stats.getDamageToHunters());
		return List.of(
			"<gray>+--------------------------------------------------+</gray>",
			" <yellow>ℹ</yellow> <white><b>THỐNG KÊ TRẬN ĐẤU</b></white>",
			"<gray>+--------------------------------------------------+</gray>",
			" <yellow>●</yellow> <white>Quái đã hạ: <green>" + stats.getMobKills() + "</green></white>",
			" <yellow>●</yellow> <white>Vào Nether: <green>" + netherTime + "</green></white>",
			" <yellow>●</yellow> <white>Vào End: <green>" + endTime + "</green></white>",
			" <yellow>●</yellow> <white>Chết tự nhiên: <green>" + stats.getNaturalDeaths() + "</green></white>",
			" <yellow>●</yellow> <white>Thẻ đã dùng: <green>" + stats.getCardsUsed() + "</green></white>",
			" <yellow>●</yellow> <white>Sát thương lên Hunter: <green>" + damage + "</green></white>",
			"<gray>+--------------------------------------------------+</gray>"
		);
	}

	private long resolveMatchDuration(Match match) {
		long start = match.getStartedAtMillis() != null
			? match.getStartedAtMillis()
			: match.getCreatedAtMillis();
		return Math.max(0L, System.currentTimeMillis() - start);
	}

	private String formatDuration(Long millis) {
		if (millis == null || millis < 0L) {
			return "Chưa";
		}
		long totalSeconds = millis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
	}

	private String resolvePlayerName(UUID playerId) {
		Player online = playerId == null ? null : Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		var offline = playerId == null ? null : Bukkit.getOfflinePlayer(playerId);
		String name = offline == null ? null : offline.getName();
		return name == null || name.isBlank() ? "Người chơi" : name;
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

	private void finishMatch(Match match, boolean clearInventory, long cleanupDelayTicks) {
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
				player.setTotalExperience(0);
				player.setLevel(0);
				player.setExp(0.0f);
				Text.msg(player, "<yellow>ℹ</yellow> Trận đấu đã kết thúc, đang đưa bạn về sảnh...");
				player.playSound(player.getLocation(), Sound.UI_TOAST_OUT, 0.7f, 1.0f);
				lobbyTeleportService.teleportToLobby(player, false);
			}
		}
		scheduleCleanup(match, cleanupDelayTicks);
	}

	private void scheduleCleanup(Match match, long delayTicks) {
		if (match == null) {
			return;
		}
		if (cleanupScheduled.putIfAbsent(match.getId(), Boolean.TRUE) != null) {
			return;
		}
		debugCleanup("Cleanup scheduled: match=" + match.getId()
			+ " delayTicks=" + delayTicks);
		new BukkitRunnable() {
			@Override
			public void run() {
				cleanupWorlds(match);
			}
		}.runTaskLater(plugin, Math.max(0L, delayTicks));
	}

	private void finalizeCleanup(Match match) {
		cleanupScheduled.remove(match.getId());
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
			finalizeCleanup(match);
			return;
		}
		if (hasPlayersInMatchWorlds(worlds)) {
			int retries = cleanupRetries.getOrDefault(match.getId(), 0);
			if (retries >= CLEANUP_RETRY_LIMIT) {
				cleanupRetries.remove(match.getId());
				debugCleanup("Cleanup deferred to queue: match=" + match.getId());
				queueWorldCleanup(worlds.getOverworldName());
				queueWorldCleanup(worlds.getNetherName());
				queueWorldCleanup(worlds.getEndName());
				finalizeCleanup(match);
				return;
			}
			cleanupRetries.put(match.getId(), retries + 1);
			debugCleanup("Cleanup delayed (players still in worlds): match=" + match.getId()
				+ " retry=" + (retries + 1) + "/" + CLEANUP_RETRY_LIMIT);
			Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupWorlds(match), CLEANUP_RETRY_DELAY_TICKS);
			return;
		}
		cleanupRetries.remove(match.getId());
		debugCleanup("Cleanup starting: match=" + match.getId());
		netherPortalsService.unlinkMatchWorlds(
			worlds.getOverworldName(),
			worlds.getNetherName(),
			worlds.getEndName()
		);
		scheduleDeleteWorld(worlds.getOverworldName(), 0);
		scheduleDeleteWorld(worlds.getNetherName(), WORLD_DELETE_SPACING_TICKS);
		scheduleDeleteWorld(worlds.getEndName(), WORLD_DELETE_SPACING_TICKS * 2L);
		finalizeCleanup(match);
	}

	private void scheduleDeleteWorld(String worldName, long delayTicks) {
		if (delayTicks <= 0L) {
			deleteWorld(worldName);
			return;
		}
		Bukkit.getScheduler().runTaskLater(plugin, () -> deleteWorld(worldName), delayTicks);
	}

	private boolean hasPlayersInMatchWorlds(MatchWorlds worlds) {
		World overworld = Bukkit.getWorld(worlds.getOverworldName());
		if (overworld != null && !overworld.getPlayers().isEmpty()) {
			return true;
		}
		World nether = Bukkit.getWorld(worlds.getNetherName());
		if (nether != null && !nether.getPlayers().isEmpty()) {
			return true;
		}
		World end = Bukkit.getWorld(worlds.getEndName());
		return end != null && !end.getPlayers().isEmpty();
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
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			if (deleteFolder(folder)) {
				removeQueuedWorld(worldName);
			} else {
				queueWorldCleanup(worldName);
			}
		});
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

	private void debugCleanup(String message) {
		plugin.getLogger().info("[CleanupDebug] " + message);
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
