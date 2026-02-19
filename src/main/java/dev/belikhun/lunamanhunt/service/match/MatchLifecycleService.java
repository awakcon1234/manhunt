package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.integration.NetherPortalsService;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.worldgen.WorldGenerationService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.title.Title;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class MatchLifecycleService {
	private static final int WORLD_TOTAL = 3;
	private static final int COUNTDOWN_SECONDS = 10;
	private static final int RUN_ACTIONBAR_TICKS = 1;

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final CompassService compassService;
	private final SpeedrunnerKitService kitService;
	private final CardService cardService;
	private final NetherPortalsService netherPortalsService;
	private final PointService pointService;
	private final Random random = new Random();
	private final ConcurrentMap<String, BukkitTask> worldProgressTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> overworldWarmupTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Integer> countdownRemainingSeconds = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> pendingActionTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> runningActionTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, WarmupStatus> warmupStatuses = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, Long> preparationStartMillis = new ConcurrentHashMap<>();

	public static final class WorldProgressSnapshot {
		private final String stageLabel;
		private final int readyWorlds;
		private final int totalWorlds;
		private final int percent;
		private final long etaSeconds;

		private WorldProgressSnapshot(String stageLabel, int readyWorlds, int totalWorlds, int percent,
				long etaSeconds) {
			this.stageLabel = stageLabel;
			this.readyWorlds = readyWorlds;
			this.totalWorlds = totalWorlds;
			this.percent = percent;
			this.etaSeconds = etaSeconds;
		}

		public String getStageLabel() {
			return stageLabel;
		}

		public int getReadyWorlds() {
			return readyWorlds;
		}

		public int getTotalWorlds() {
			return totalWorlds;
		}

		public int getPercent() {
			return percent;
		}

		public long getEtaSeconds() {
			return etaSeconds;
		}
	}

	public MatchLifecycleService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager,
			CompassService compassService, SpeedrunnerKitService kitService, CardService cardService,
			NetherPortalsService netherPortalsService, PointService pointService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
		this.kitService = Objects.requireNonNull(kitService, "kitService");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.netherPortalsService = Objects.requireNonNull(netherPortalsService, "netherPortalsService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
	}

	public void handleWorldsCreating(Match match) {
		if (match == null || match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
		preparationStartMillis.putIfAbsent(match.getId(), System.currentTimeMillis());
		broadcast(match, "<yellow>ℹ</yellow> Trận đấu đang được chuẩn bị, hãy sẵn sàng!");
		playMatchSound(match, Sound.UI_TOAST_IN, 0.7f, 1.0f);
		sendActionBar(match, "<yellow>⌛</yellow> Đang tạo thế giới: <white>0/" + WORLD_TOTAL + "</white>");
		startWorldProgressTask(match);
	}

	public void handleWorldGenerated(Match match, LoadedMultiverseWorld world) {
		if (match == null || world == null) {
			return;
		}
		World bukkitWorld = Bukkit.getWorld(world.getName());
		if (bukkitWorld == null) {
			return;
		}
		applyWorldRule(bukkitWorld, match.getSettings());
		recordWorldSeed(match, bukkitWorld);
		String label = formatWorldLabel(bukkitWorld.getEnvironment());
		String seed = formatSeedValue(bukkitWorld.getSeed());
		broadcast(match, "<green>✔</green> Thế giới <yellow>" + label + "</yellow> đã tạo xong. Seed: " + seed + ".");
		playMatchSound(match, Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
	}

	public void handleWorldsReady(Match match) {
		if (match == null || match.getState() != MatchState.WORLD_READY) {
			return;
		}
		MatchWorlds worlds = match.getWorlds();
		if (worlds == null) {
			markMatchFailed(match, "<red>❌</red> Không thể khởi tạo thế giới trận đấu.");
			return;
		}
		World overworld = Bukkit.getWorld(worlds.getOverworldName());
		if (overworld == null) {
			markMatchFailed(match, "<red>❌</red> Thế giới trận đấu chưa sẵn sàng.");
			return;
		}
		recordWorldSeed(match, overworld);

		UUID speedrunnerId = resolveSpeedrunner(match);
		if (speedrunnerId == null) {
			markMatchFailed(match, "<red>❌</red> Không có người chơi để bắt đầu trận đấu.");
			return;
		}
		match.setSpeedrunnerId(speedrunnerId);
		assignHunters(match, speedrunnerId);
		broadcast(match, "<yellow>ℹ</yellow> " + UiColors.roleSpeedrunner()
			+ ": <yellow>" + resolvePlayerName(speedrunnerId) + "</yellow>.");
		startOverworldWarmup(match, overworld);
	}

	private void recordWorldSeed(Match match, World world) {
		if (match == null || world == null) {
			return;
		}
		long seed = world.getSeed();
		switch (world.getEnvironment()) {
			case NORMAL -> match.setOverworldSeed(seed);
			case NETHER -> match.setNetherSeed(seed);
			case THE_END -> match.setEndSeed(seed);
			default -> {
			}
		}
	}

	private void linkMatchPortals(MatchWorlds worlds) {
		if (worlds == null) {
			return;
		}
		netherPortalsService.linkMatchWorlds(
			worlds.getOverworldName(),
			worlds.getNetherName(),
			worlds.getEndName()
		);
	}

	public void handleWorldCreationFailed(Match match, String worldName) {
		if (match == null) {
			return;
		}
		String label = resolveWorldLabel(worldName);
		markMatchFailed(match, "<red>❌</red> Không thể tạo thế giới <yellow>" + label + "</yellow>. Vui lòng thử lại.");
	}

	public boolean tryStartRun(Player attacker, Player victim) {
		if (attacker == null || victim == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(attacker.getUniqueId());
		if (match == null || match.getState() != MatchState.RUN_PENDING) {
			return false;
		}
		if (!match.getId().equals(getMatchId(victim))) {
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(attacker.getUniqueId())) {
			return false;
		}
		if (!match.getHunters().contains(victim.getUniqueId())) {
			return false;
		}

		if (!matchManager.beginRun(match)) {
			return false;
		}
		clearMatchAdvancements(match);
		clearMatchExperience(match);
		stopPendingActionTask(match.getId());
		applyWorldBorderLimit(match);
		setGameMode(match, GameMode.SURVIVAL);
		for (UUID hunterId : Set.copyOf(match.getHunters())) {
			Player hunter = Bukkit.getPlayer(hunterId);
			if (hunter != null) {
				compassService.giveTrackingCompass(hunter, match);
			}
		}
		cardService.addPoint(match, 1);
		cardService.drawNewCards(match);
		Player speedrunner = Bukkit.getPlayer(match.getSpeedrunnerId());
		if (speedrunner != null) {
			Text.msg(speedrunner, "<green>✔</green> Bạn nhận được <white>1</white> điểm khởi đầu và <white>7</white> thẻ mới.");
			Text.msg(speedrunner, "<yellow>ℹ</yellow> Dùng " + UiColors.command("/c") + " để sử dụng điểm.");
			speedrunner.playSound(speedrunner.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
		}
		playMatchStartSound(match);
		broadcast(match, "<green>✔</green> Trận đấu bắt đầu. Chúc may mắn!");
		startRunningActionTask(match);
		return true;
	}

	private void clearMatchAdvancements(Match match) {
		if (match == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				clearPlayerAdvancements(player);
			}
		}
	}

	private void clearPlayerAdvancements(Player player) {
		if (player == null) {
			return;
		}
		for (java.util.Iterator<Advancement> it = Bukkit.advancementIterator(); it.hasNext(); ) {
			Advancement advancement = it.next();
			AdvancementProgress progress = player.getAdvancementProgress(advancement);
			for (String criteria : List.copyOf(progress.getAwardedCriteria())) {
				progress.revokeCriteria(criteria);
			}
		}
	}

	private void clearMatchExperience(Match match) {
		if (match == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				clearPlayerExperience(player);
			}
		}
	}

	private void clearPlayerExperience(Player player) {
		if (player == null) {
			return;
		}
		player.setTotalExperience(0);
		player.setLevel(0);
		player.setExp(0.0f);
	}

	public boolean shouldCancelPendingDamage(Player attacker, Player victim) {
		if (attacker == null || victim == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(attacker.getUniqueId());
		if (match == null || match.getState() != MatchState.RUN_PENDING) {
			return false;
		}
		if (!match.getId().equals(getMatchId(victim))) {
			return false;
		}
		if (match.getHunters().contains(attacker.getUniqueId())) {
			return true;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null && speedrunnerId.equals(attacker.getUniqueId())) {
			return !match.getHunters().contains(victim.getUniqueId());
		}
		return true;
	}

	private void markMatchFailed(Match match, String message) {
		match.setState(MatchState.FINISHED);
		match.setEndedAtMillis(System.currentTimeMillis());
		stopWorldProgressTask(match.getId());
		stopOverworldWarmupTask(match.getId());
		stopCountdownTask(match.getId());
		stopPendingActionTask(match.getId());
		stopRunningActionTask(match.getId());
		warmupStatuses.remove(match.getId());
		preparationStartMillis.remove(match.getId());
		broadcast(match, message);
		lobbyManager.disbandMatch(match);
	}

	private UUID resolveSpeedrunner(Match match) {
		Set<UUID> lobbyPlayers = match.getLobbyPlayers();
		if (lobbyPlayers.isEmpty()) {
			return null;
		}
		MatchSettings settings = match.getSettings();
		UUID selected = settings.getSpeedrunnerId();
		if (settings.getSpeedrunnerSelection() == SpeedrunnerSelection.SPECIFIC) {
			if (selected != null && lobbyPlayers.contains(selected)) {
				return selected;
			}
		}
		List<UUID> candidates = new ArrayList<>(lobbyPlayers);
		return candidates.get(random.nextInt(candidates.size()));
	}

	private void assignHunters(Match match, UUID speedrunnerId) {
		match.getHunters().clear();
		match.getHunters().addAll(match.getLobbyPlayers());
		match.getHunters().remove(speedrunnerId);
	}

	private void applyWorldBorder(World world, Location center, MatchSettings settings) {
		if (world == null || center == null || settings == null) {
			return;
		}
		var border = world.getWorldBorder();
		border.setCenter(center);
		border.setSize(settings.getStartingBorder());
	}

	private void applyWorldBorderLimit(Match match) {
		if (match == null || match.getWorlds() == null) {
			return;
		}
		World overworld = Bukkit.getWorld(match.getWorlds().getOverworldName());
		if (overworld == null) {
			return;
		}
		var border = overworld.getWorldBorder();
		border.setSize(match.getSettings().getWorldBorderLimit());
	}

	private void applyWorldRule(World world, MatchSettings settings) {
		if (world == null) {
			return;
		}
		world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
		world.setGameRule(GameRules.LOCATOR_BAR, false);
		if (settings == null) {
			return;
		}
		applyKeepInventory(world, settings);
		applyWorldTime(world, settings);
		applyWorldWeather(world, settings);
		applyWorldDifficulty(world, settings);
	}

	private void applyKeepInventory(World world, MatchSettings settings) {
		if (world == null || settings == null) {
			return;
		}
		world.setGameRule(GameRules.KEEP_INVENTORY, settings.isKeepInventory());
	}

	private void applyWorldTime(World world, MatchSettings settings) {
		if (world == null || settings == null || settings.getWorldTime() == null) {
			return;
		}
			switch (settings.getWorldTime()) {
				case CYCLE -> world.setGameRule(GameRules.ADVANCE_TIME, true);
				case MORNING -> {
					world.setGameRule(GameRules.ADVANCE_TIME, false);
					world.setTime(1000L);
				}
				case NOON -> {
					world.setGameRule(GameRules.ADVANCE_TIME, false);
					world.setTime(6000L);
				}
				case NIGHT -> {
					world.setGameRule(GameRules.ADVANCE_TIME, false);
					world.setTime(13000L);
				}
				case MIDNIGHT -> {
					world.setGameRule(GameRules.ADVANCE_TIME, false);
					world.setTime(18000L);
				}
			}
	}

	private void applyWorldWeather(World world, MatchSettings settings) {
		if (world == null || settings == null || settings.getWorldWeather() == null) {
			return;
		}
			switch (settings.getWorldWeather()) {
				case CYCLE -> world.setGameRule(GameRules.ADVANCE_WEATHER, true);
				case CLEAR -> {
					world.setGameRule(GameRules.ADVANCE_WEATHER, false);
					world.setStorm(false);
					world.setThundering(false);
					world.setWeatherDuration(Integer.MAX_VALUE);
					world.setThunderDuration(0);
				}
				case RAIN -> {
					world.setGameRule(GameRules.ADVANCE_WEATHER, false);
					world.setStorm(true);
					world.setThundering(false);
					world.setWeatherDuration(Integer.MAX_VALUE);
					world.setThunderDuration(0);
				}
				case THUNDERSTORM -> {
					world.setGameRule(GameRules.ADVANCE_WEATHER, false);
					world.setStorm(true);
					world.setThundering(true);
					world.setWeatherDuration(Integer.MAX_VALUE);
					world.setThunderDuration(Integer.MAX_VALUE);
				}
			}
	}

	private void applyWorldDifficulty(World world, MatchSettings settings) {
		if (world == null || settings == null || settings.getWorldDifficulty() == null) {
			return;
		}
		Difficulty difficulty = switch (settings.getWorldDifficulty()) {
			case PEACEFUL -> Difficulty.PEACEFUL;
			case EASY -> Difficulty.EASY;
			case HARD -> Difficulty.HARD;
			case MEDIUM -> Difficulty.NORMAL;
		};
		world.setDifficulty(difficulty);
	}

	private void teleportPlayers(Match match, Location spawn) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.teleport(spawn);
			}
		}
	}

	private void clearForcedRespawnPoint(Match match) {
		if (match == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.setRespawnLocation(null, true);
			}
		}
	}

	private void clearLobbyItems(Match match) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) {
				continue;
			}
			player.getInventory().clear();
			player.getInventory().setArmorContents(null);
			player.getInventory().setExtraContents(null);
		}
	}

	private void setGameMode(Match match, GameMode gameMode) {
		if (match == null || gameMode == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.setGameMode(gameMode);
			}
		}
	}

	private void giveSpeedrunnerKit(Match match, UUID speedrunnerId) {
		Player speedrunner = Bukkit.getPlayer(speedrunnerId);
		if (speedrunner == null) {
			return;
		}
		List<ItemStack> items = kitService.resolveKitItems(match.getSettings());
		if (items.isEmpty()) {
			return;
		}
		PlayerInventory inventory = speedrunner.getInventory();
		for (ItemStack item : items) {
			if (item != null) {
				if (equipArmorIfPossible(inventory, item)) {
					continue;
				}
				inventory.addItem(item);
			}
		}
	}

	private boolean equipArmorIfPossible(PlayerInventory inventory, ItemStack item) {
		if (inventory == null || item == null) {
			return false;
		}
		Material type = item.getType();
		if (type == Material.AIR) {
			return false;
		}
		if (type == Material.ELYTRA || type.name().endsWith("_CHESTPLATE")) {
			if (isEmptyArmor(inventory.getChestplate())) {
				inventory.setChestplate(item);
				return true;
			}
			return false;
		}
		if (type == Material.TURTLE_HELMET || type.name().endsWith("_HELMET")) {
			if (isEmptyArmor(inventory.getHelmet())) {
				inventory.setHelmet(item);
				return true;
			}
			return false;
		}
		if (type.name().endsWith("_LEGGINGS")) {
			if (isEmptyArmor(inventory.getLeggings())) {
				inventory.setLeggings(item);
				return true;
			}
			return false;
		}
		if (type.name().endsWith("_BOOTS")) {
			if (isEmptyArmor(inventory.getBoots())) {
				inventory.setBoots(item);
				return true;
			}
			return false;
		}
		return false;
	}

	private boolean isEmptyArmor(ItemStack item) {
		return item == null || item.getType() == Material.AIR;
	}

	private String getMatchId(Player player) {
		Match victimMatch = lobbyManager.getMatchForPlayer(player.getUniqueId());
		return victimMatch == null ? null : victimMatch.getId();
	}

	private void broadcast(Match match, String message) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				Text.msg(player, message);
			}
		}
	}

	private void startWorldProgressTask(Match match) {
		stopWorldProgressTask(match.getId());
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			MatchWorlds worlds = match.getWorlds();
			boolean warmupActive = warmupStatuses.containsKey(match.getId());
			if ((match.getState() != MatchState.WORLD_CREATING && !warmupActive) || worlds == null) {
				stopWorldProgressTask(match.getId());
				return;
			}
			sendActionBar(match, buildWorldProgressMessage(match, worlds));
		}, 0L, 20L);
		worldProgressTasks.put(match.getId(), task);
	}

	private void stopWorldProgressTask(String matchId) {
		stopTask(worldProgressTasks, matchId);
	}

	public void updateWorldProgressActionBar(String matchId) {
		if (matchId == null) {
			return;
		}
		Match match = matchManager.getMatch(matchId);
		boolean warmupActive = warmupStatuses.containsKey(matchId);
		if (match == null || (match.getState() != MatchState.WORLD_CREATING && !warmupActive)) {
			return;
		}
		MatchWorlds worlds = match.getWorlds();
		if (worlds == null) {
			return;
		}
		sendActionBar(match, buildWorldProgressMessage(match, worlds));
	}

	public WorldProgressSnapshot getWorldProgressSnapshot(String matchId) {
		if (matchId == null) {
			return null;
		}
		Match match = matchManager.getMatch(matchId);
		if (match == null || match.getWorlds() == null) {
			return null;
		}
		MatchWorlds worlds = match.getWorlds();
		return buildWorldProgressSnapshot(match, worlds);
	}

	public int getCountdownRemainingSeconds(String matchId) {
		if (matchId == null) {
			return -1;
		}
		Integer value = countdownRemainingSeconds.get(matchId);
		return value == null ? -1 : value;
	}

	private String buildWorldProgressMessage(Match match, MatchWorlds worlds) {
		WorldProgressSnapshot snapshot = buildWorldProgressSnapshot(match, worlds);
		if (snapshot == null) {
			return "<yellow>⌛</yellow> <gold>Đang chuẩn bị...</gold>";
		}
		WorldGenerationService.GenerationProgress progress = matchManager.getGenerationProgress(match.getId());
		boolean isGenerating = progress != null
			&& progress.getStage() == WorldGenerationService.GenerationStage.GENERATING;
		String countPart = isGenerating
			? " <gray>(" + snapshot.getReadyWorlds() + "/" + snapshot.getTotalWorlds() + ")</gray>"
			: "";
		return "<yellow>⌛</yellow> <gold>" + snapshot.getStageLabel() + "...</gold>" + countPart
			+ " <dark_gray>●</dark_gray> <green>" + snapshot.getPercent() + "%</green> <dark_gray>●</dark_gray>"
			+ " <yellow>⌚</yellow> <white>" + formatEta(snapshot.getEtaSeconds())
			+ "</white> <gray>còn lại</gray>";
	}

	private WorldProgressSnapshot buildWorldProgressSnapshot(Match match, MatchWorlds worlds) {
		if (match == null || worlds == null) {
			return null;
		}
		WorldGenerationService.GenerationProgress progress = matchManager.getGenerationProgress(match.getId());
		WarmupStatus warmupStatus = warmupStatuses.get(match.getId());
		int ready = countReady(worlds);
		String stageLabel = warmupStatus != null
			? "Đang chuẩn bị Overworld"
			: (progress == null ? "Đang tạo thế giới" : formatWorldGenStage(progress.getStage()));
		long warmupRemaining = warmupStatus != null ? warmupStatus.getRemainingSeconds() : 0L;
		long warmupTotal = warmupStatus != null
			? warmupStatus.getTotalSeconds()
			: estimateWarmupTotalSeconds();
		long prepStart = preparationStartMillis.getOrDefault(match.getId(), System.currentTimeMillis());
		long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - prepStart) / 1000L);
		long worldgenRemaining = progress == null ? 0L : progress.getEtaSeconds();
		double worldgenWeight = warmupStatus != null
			? warmupStatus.getWorldgenWeight()
			: computeWorldgenWeight(elapsedSeconds, worldgenRemaining, warmupTotal);
		double overallProgress;
		long totalRemaining;
		if (warmupStatus != null) {
			overallProgress = worldgenWeight + warmupStatus.getProgress() * (1.0 - worldgenWeight);
			totalRemaining = warmupRemaining;
		} else {
			overallProgress = (progress == null ? 0.0 : progress.getOverallProgress()) * worldgenWeight;
			totalRemaining = worldgenRemaining + warmupTotal;
		}
		int percent = (int) Math.round(Math.min(1.0, Math.max(0.0, overallProgress)) * 100.0);
		return new WorldProgressSnapshot(stageLabel, ready, WORLD_TOTAL, percent, totalRemaining);
	}

	private String formatWorldGenStage(WorldGenerationService.GenerationStage stage) {
		if (stage == null) {
			return "Đang chuẩn bị";
		}
		return switch (stage) {
			case DOWNLOADING -> "Đang tải server";
			case SETUP -> "Đang thiết lập";
			case BOOTING -> "Đang khởi động";
			case STARTING -> "Đang bắt đầu";
			case GENERATING -> "Đang tạo thế giới";
			case SAVING -> "Đang lưu";
			case REGISTERING -> "Đang đăng ký";
			case DONE -> "Hoàn tất";
		};
	}

	private String formatEta(long seconds) {
		if (seconds <= 0) {
			return "??:??s";
		}
		Duration duration = Duration.ofSeconds(seconds);
		long minutes = duration.toMinutes();
		long remaining = duration.minusMinutes(minutes).toSeconds();
		return String.format(java.util.Locale.US, "%02d:%02ds", minutes, remaining);
	}

	private long estimateWarmupEtaSeconds(int totalChunks, int completedChunks, int batchSize) {
		if (totalChunks <= 0 || batchSize <= 0) {
			return 0L;
		}
		int remaining = Math.max(0, totalChunks - completedChunks);
		int ticksRemaining = (int) Math.ceil(remaining / (double) batchSize);
		return Math.max(0L, Math.round(ticksRemaining / 20.0));
	}

	private long estimateWarmupTotalSeconds() {
		int totalChunks = 7 * 7;
		return estimateWarmupEtaSeconds(totalChunks, 0, 2);
	}

	private double computeWorldgenWeight(long elapsedSeconds, long remainingSeconds, long warmupSeconds) {
		double worldgenTotal = Math.max(1.0, elapsedSeconds + Math.max(0L, remainingSeconds));
		double warmupTotal = Math.max(1.0, warmupSeconds);
		return worldgenTotal / (worldgenTotal + warmupTotal);
	}

	private double computeWorldgenWeightForWarmup(String matchId, long warmupSeconds) {
		long prepStart = preparationStartMillis.getOrDefault(matchId, System.currentTimeMillis());
		long elapsedSeconds = Math.max(0L, (System.currentTimeMillis() - prepStart) / 1000L);
		WorldGenerationService.GenerationProgress progress = matchManager.getGenerationProgress(matchId);
		long remainingSeconds = progress == null ? 0L : progress.getEtaSeconds();
		return computeWorldgenWeight(elapsedSeconds, remainingSeconds, warmupSeconds);
	}

	private static class WarmupStatus {
		private final int totalChunks;
		private final int batchSize;
		private final double worldgenWeight;
		private volatile int completedChunks;
		private volatile long lastUpdateMillis;
		private volatile int lastCompletedChunks;
		private volatile double chunksPerSecond;

		private WarmupStatus(int totalChunks, int batchSize, double worldgenWeight) {
			this.totalChunks = totalChunks;
			this.batchSize = batchSize;
			this.worldgenWeight = Math.min(1.0, Math.max(0.0, worldgenWeight));
		}

		void updateRate(int completedChunks, long nowMillis) {
			this.completedChunks = Math.min(totalChunks, Math.max(0, completedChunks));
			if (lastUpdateMillis <= 0) {
				lastUpdateMillis = nowMillis;
				lastCompletedChunks = this.completedChunks;
				return;
			}
			long deltaMillis = nowMillis - lastUpdateMillis;
			int deltaChunks = this.completedChunks - lastCompletedChunks;
			if (deltaMillis <= 0 || deltaChunks <= 0) {
				return;
			}
			double currentRate = (deltaChunks * 1000.0) / deltaMillis;
			chunksPerSecond = chunksPerSecond <= 0.0
				? currentRate
				: chunksPerSecond * 0.7 + currentRate * 0.3;
			lastUpdateMillis = nowMillis;
			lastCompletedChunks = this.completedChunks;
		}

		double getProgress() {
			if (totalChunks <= 0) {
				return 0.0;
			}
			return Math.min(1.0, Math.max(0.0, completedChunks / (double) totalChunks));
		}

		long getRemainingSeconds() {
			int remaining = Math.max(0, totalChunks - completedChunks);
			if (chunksPerSecond > 0.0) {
				double seconds = remaining / chunksPerSecond;
				return Math.max(0L, Math.round(seconds));
			}
			int ticksRemaining = (int) Math.ceil(remaining / (double) Math.max(1, batchSize));
			return Math.max(0L, Math.round(ticksRemaining / 20.0));
		}

		long getTotalSeconds() {
			int ticksTotal = (int) Math.ceil(totalChunks / (double) Math.max(1, batchSize));
			return Math.max(0L, Math.round(ticksTotal / 20.0));
		}

		double getWorldgenWeight() {
			return worldgenWeight;
		}
	}

	private void startOverworldWarmup(Match match, World overworld) {
		stopOverworldWarmupTask(match.getId());
		Location spawn = overworld.getSpawnLocation();
		int centerX = spawn.getBlockX() >> 4;
		int centerZ = spawn.getBlockZ() >> 4;
		List<int[]> chunks = new ArrayList<>();
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -3; dz <= 3; dz++) {
				chunks.add(new int[] { centerX + dx, centerZ + dz });
			}
		}
		int total = chunks.size();
		int batch = 2;
		WarmupStatus warmupStatus = new WarmupStatus(total, batch,
			computeWorldgenWeightForWarmup(match.getId(), estimateWarmupEtaSeconds(total, 0, batch)));
		warmupStatuses.put(match.getId(), warmupStatus);
		updateWorldProgressActionBar(match.getId());

		BukkitRunnable runnable = new BukkitRunnable() {
			int requested = 0;
			int completed = 0;

			@Override
			public void run() {
				if (match.getState() != MatchState.WORLD_READY) {
					stopOverworldWarmupTask(match.getId());
					warmupStatuses.remove(match.getId());
					cancel();
					return;
				}
				int inFlight = requested - completed;
				int toRequest = Math.min(batch - Math.max(0, inFlight), total - requested);
				for (int i = 0; i < toRequest; i++) {
					int[] coord = chunks.get(requested++);
					overworld.getChunkAtAsync(coord[0], coord[1], true)
						.whenComplete((chunk, error) -> Bukkit.getScheduler().runTask(plugin, () -> {
							if (match.getState() != MatchState.WORLD_READY) {
								return;
							}
							if (error != null) {
								plugin.getLogger().warning("Failed to warm up chunk " + coord[0] + "," + coord[1]
									+ ": " + error.getMessage());
							}
							completed++;
							warmupStatus.updateRate(completed, System.currentTimeMillis());
							updateWorldProgressActionBar(match.getId());
						}));
				}
				if (completed >= total) {
					stopOverworldWarmupTask(match.getId());
					warmupStatuses.remove(match.getId());
					preparationStartMillis.remove(match.getId());
					stopWorldProgressTask(match.getId());
					clearActionBar(match);
					cancel();
					broadcast(match, "<green>✔</green> Overworld đã sẵn sàng. Chuẩn bị đếm ngược...");
					playMatchSound(match, Sound.UI_BUTTON_CLICK, 0.7f, 1.1f);
					startCountdown(match, overworld);
				}
			}
		};
		BukkitTask task = runnable.runTaskTimer(plugin, 0L, 1L);
		overworldWarmupTasks.put(match.getId(), task);
	}

	private void stopOverworldWarmupTask(String matchId) {
		stopTask(overworldWarmupTasks, matchId);
	}

	private void startCountdown(Match match, World overworld) {
		stopCountdownTask(match.getId());
		MatchWorlds worlds = match.getWorlds();
		if (worlds != null) {
			linkMatchPortals(worlds);
		}
		broadcast(match, "<yellow>⌛</yellow> Bắt đầu đếm ngược <white>" + COUNTDOWN_SECONDS + "</white> giây.");
		playMatchSound(match, Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
		countdownRemainingSeconds.put(match.getId(), COUNTDOWN_SECONDS);
		BukkitRunnable runnable = new BukkitRunnable() {
			int seconds = COUNTDOWN_SECONDS;

			@Override
			public void run() {
				if (match.getState() != MatchState.WORLD_READY) {
					stopCountdownTask(match.getId());
					cancel();
					return;
				}
				if (seconds <= 0) {
					stopCountdownTask(match.getId());
					prepareRunPending(match, overworld);
					cancel();
					return;
				}
				countdownRemainingSeconds.put(match.getId(), seconds);
				showCountdownSubtitle(match, seconds);
				playCountdownSound(match, seconds);
				seconds--;
			}
		};
		BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
		countdownTasks.put(match.getId(), task);
	}

	private void stopCountdownTask(String matchId) {
		countdownRemainingSeconds.remove(matchId);
		stopTask(countdownTasks, matchId);
	}

	private void prepareRunPending(Match match, World overworld) {
		if (match == null || overworld == null) {
			return;
		}
		Location spawn = overworld.getSpawnLocation();
		applyWorldBorder(overworld, spawn, match.getSettings());
		teleportPlayers(match, spawn);
		clearForcedRespawnPoint(match);
		clearLobbyItems(match);
		setGameMode(match, GameMode.ADVENTURE);
		giveSpeedrunnerKit(match, match.getSpeedrunnerId());
		match.setState(MatchState.RUN_PENDING);
		broadcastMatchInfo(match, overworld);
		broadcast(match, "<green>✔</green> Đã đưa vào thế giới trận đấu. Hãy sẵn sàng!");
		playMatchSound(match, Sound.UI_TOAST_IN, 0.7f, 1.0f);
		sendRunPendingGuide(match);
		startPendingActionTask(match);
	}

	private void sendRunPendingGuide(Match match) {
		if (match == null) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player == null) {
				continue;
			}
			boolean isSpeedrunner = speedrunnerId != null && speedrunnerId.equals(id);
			sendRunPendingGuideLines(player, isSpeedrunner);
		}
	}

	private void sendRunPendingGuideLines(Player player, boolean isSpeedrunner) {
		Text.tell(player, "");
		Text.tell(player, "");
		Text.tell(player, " <yellow>ℹ</yellow> <white><b>HƯỚNG DẪN NHANH</b></white>");
		Text.tell(player, "");
		Text.tell(player, " <yellow>●</yellow> <white>Mục tiêu: " + UiColors.roleSpeedrunnerBold()
			+ " hạ " + UiColors.speedrunner("<b>Ender Dragon</b>") + "</white>");
		Text.tell(player, " <yellow>●</yellow> <white>Điều kiện thắng: " + UiColors.roleHuntersBold()
			+ " giết " + UiColors.roleSpeedrunnerBold() + "</white>");
		Text.tell(player, " <yellow>●</yellow> <white>Bắt đầu: " + UiColors.roleSpeedrunnerBold()
			+ " đánh một " + UiColors.roleHunterBold() + "</white>");
		Text.tell(player, " <yellow>●</yellow> <white>" + UiColors.roleHuntersBold()
			+ " nhận " + UiColors.hunter("<b>la bàn truy vết</b>") + " khi bắt đầu</white>");
		if (isSpeedrunner) {
			Text.tell(player, " <yellow>●</yellow> <white>Vai trò: " + UiColors.speedrunner("<b>kết liễu rồng</b>") + " và sinh tồn</white>");
		} else {
			Text.tell(player, " <yellow>●</yellow> <white>Vai trò: " + UiColors.hunter("<b>hạ</b>") + " "
				+ UiColors.roleSpeedrunnerBold() + " " + UiColors.hunter("<b>bằng truy đuổi</b>") + "</white>");
		}
		Text.tell(player, "");
	}

	private void startPendingActionTask(Match match) {
		stopPendingActionTask(match.getId());
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (match.getState() != MatchState.RUN_PENDING) {
				stopPendingActionTask(match.getId());
				return;
			}
			UUID speedrunnerId = match.getSpeedrunnerId();
			for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
				Player player = Bukkit.getPlayer(id);
				if (player == null) {
					continue;
				}
				if (player.getGameMode() != GameMode.ADVENTURE) {
					player.setGameMode(GameMode.ADVENTURE);
				}
				String message = (speedrunnerId != null && speedrunnerId.equals(id))
					? "<yellow>ℹ</yellow> <white>Bạn là " + UiColors.roleSpeedrunner()
						+ "</white> <gray>•</gray> <white>Hãy đánh một " + UiColors.roleHunterLower() + " để bắt đầu</white>"
					: "<yellow>ℹ</yellow> <white>Chờ " + UiColors.roleSpeedrunner()
						+ " đánh một " + UiColors.roleHunterLower() + " để bắt đầu trận đấu</white>";
				player.sendActionBar(Text.mm(message));
			}
		}, 0L, RUN_ACTIONBAR_TICKS);
		pendingActionTasks.put(match.getId(), task);
	}

	private void stopPendingActionTask(String matchId) {
		stopTask(pendingActionTasks, matchId);
	}

	private void startRunningActionTask(Match match) {
		stopRunningActionTask(match.getId());
		BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
			if (match.getState() != MatchState.RUNNING) {
				stopRunningActionTask(match.getId());
				return;
			}
			Long startedAt = match.getStartedAtMillis();
			if (startedAt == null) {
				return;
			}
			UUID speedrunnerId = match.getSpeedrunnerId();
			String timeText = formatRunTime(System.currentTimeMillis() - startedAt);
			for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
				Player player = Bukkit.getPlayer(id);
				if (player == null) {
					continue;
				}
				String message = "<#32faa7>⌚</#32faa7> <white>" + timeText + "</white>";
				if (speedrunnerId != null && speedrunnerId.equals(id)) {
					message = message + " <gray>•</gray> <white>" + pointService.formatPoints(match.getSpeedrunnerPoints())
						+ "</white>";
				}
				player.sendActionBar(Text.mm(message));
			}
		}, 0L, RUN_ACTIONBAR_TICKS);
		runningActionTasks.put(match.getId(), task);
	}

	private void stopRunningActionTask(String matchId) {
		stopTask(runningActionTasks, matchId);
	}

	private void showCountdownSubtitle(Match match, int seconds) {
		String color = getCountdownColor(seconds);
		Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO);
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.showTitle(Title.title(net.kyori.adventure.text.Component.empty(),
					Text.mm("<" + color + ">" + seconds + "</" + color + ">"), times));
			}
		}
	}

	private void playCountdownSound(Match match, int seconds) {
		Sound sound = getCountdownSound(seconds);
		float pitch = getCountdownPitch(seconds);
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.playSound(player.getLocation(), sound, 1.0f, pitch);
			}
		}
	}

	private String getCountdownColor(int seconds) {
		return switch (seconds) {
			case 3 -> "yellow";
			case 2 -> "#f59e0b";
			case 1 -> "red";
			default -> "yellow";
		};
	}

	private Sound getCountdownSound(int seconds) {
		return switch (seconds) {
			case 3 -> Sound.BLOCK_NOTE_BLOCK_PLING;
			case 2 -> Sound.BLOCK_NOTE_BLOCK_BELL;
			case 1 -> Sound.BLOCK_NOTE_BLOCK_BIT;
			default -> Sound.BLOCK_NOTE_BLOCK_HAT;
		};
	}

	private float getCountdownPitch(int seconds) {
		return switch (seconds) {
			case 3 -> 1.2f;
			case 2 -> 1.4f;
			case 1 -> 1.6f;
			default -> 1.0f;
		};
	}

	private void playMatchStartSound(Match match) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
			}
		}
	}

	private void broadcastMatchInfo(Match match, World overworld) {
		String seed = formatSeedValue(overworld.getSeed());
		broadcast(match, "<yellow>ℹ</yellow> Thông tin trận đấu:");
		broadcast(match, "<gray>● Mã phòng: <white>" + match.getId() + "</white></gray>");
		broadcast(match, "<gray>● Seed: " + seed + "</gray>");
	}

	private void sendActionBar(Match match, String message) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.sendActionBar(Text.mm(message));
			}
		}
	}

	private void playMatchSound(Match match, Sound sound, float volume, float pitch) {
		if (match == null || sound == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.playSound(player.getLocation(), sound, volume, pitch);
			}
		}
	}

	private String resolvePlayerName(UUID playerId) {
		Player online = Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		var offline = Bukkit.getOfflinePlayer(playerId);
		String name = offline == null ? null : offline.getName();
		return name == null || name.isBlank() ? "Người chơi" : name;
	}

	private void clearActionBar(Match match) {
		sendActionBar(match, "");
	}

	private void stopTask(ConcurrentMap<String, BukkitTask> tasks, String matchId) {
		BukkitTask task = tasks.remove(matchId);
		if (task != null) {
			task.cancel();
		}
	}

	private int countReady(MatchWorlds worlds) {
		int ready = 0;
		if (worlds.isOverworldReady()) {
			ready++;
		}
		if (worlds.isNetherReady()) {
			ready++;
		}
		if (worlds.isEndReady()) {
			ready++;
		}
		return ready;
	}

	private String resolveWorldLabel(String worldName) {
		if (worldName == null) {
			return "thế giới";
		}
		String lower = worldName.toLowerCase();
		if (lower.endsWith("_overworld")) {
			return "Overworld";
		}
		if (lower.endsWith("_nether")) {
			return "Nether";
		}
		if (lower.endsWith("_end")) {
			return "The End";
		}
		return "thế giới";
	}

	private String formatWorldLabel(World.Environment environment) {
		return switch (environment) {
			case NORMAL -> "Overworld";
			case NETHER -> "Nether";
			case THE_END -> "The End";
			default -> "Thế giới";
		};
	}

	private String formatRunTime(long millis) {
		long safeMillis = Math.max(0, millis);
		long totalSeconds = safeMillis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		long milli = safeMillis % 1000L;
		return String.format("%02d:%02d.%03d", minutes, seconds, milli);
	}

	private String formatSeedValue(long seed) {
		return "<click:copy_to_clipboard:'" + seed + "'><hover:show_text:'<gray>Bấm để sao chép seed</gray>'><#36a8ff>" + seed + "</#36a8ff></hover></click>";
	}
}
