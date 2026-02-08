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
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Bukkit;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
	private static final int RUN_ACTIONBAR_TICKS = 2;

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final CompassService compassService;
	private final SpeedrunnerKitService kitService;
	private final CardService cardService;
	private final NetherPortalsService netherPortalsService;
	private final Random random = new Random();
	private final ConcurrentMap<String, BukkitTask> worldProgressTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> overworldWarmupTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> pendingActionTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> runningActionTasks = new ConcurrentHashMap<>();

	public MatchLifecycleService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager,
			CompassService compassService, SpeedrunnerKitService kitService, CardService cardService,
			NetherPortalsService netherPortalsService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
		this.kitService = Objects.requireNonNull(kitService, "kitService");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.netherPortalsService = Objects.requireNonNull(netherPortalsService, "netherPortalsService");
	}

	public void handleWorldsCreating(Match match) {
		if (match == null || match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
		broadcast(match, "<yellow>ℹ</yellow> Trận đấu đang được chuẩn bị, hãy sẵn sàng!");
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
		applyWorldRule(bukkitWorld);
		String label = formatWorldLabel(bukkitWorld.getEnvironment());
		String seed = formatSeedValue(bukkitWorld.getSeed());
		broadcast(match, "<green>✔</green> Thế giới <yellow>" + label + "</yellow> đã tạo xong. Seed: " + seed + ".");
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

		stopWorldProgressTask(match.getId());
		UUID speedrunnerId = resolveSpeedrunner(match);
		if (speedrunnerId == null) {
			markMatchFailed(match, "<red>❌</red> Không có người chơi để bắt đầu trận đấu.");
			return;
		}
		match.setSpeedrunnerId(speedrunnerId);
		assignHunters(match, speedrunnerId);
		linkMatchPortals(worlds);
		startOverworldWarmup(match, overworld);
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
		}
		playMatchStartSound(match);
		broadcast(match, "<green>✔</green> Trận đấu bắt đầu. Chúc may mắn!");
		startRunningActionTask(match);
		return true;
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
		broadcast(match, message);
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

	private void applyWorldRule(World world) {
		if (world == null) {
			return;
		}
		world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
		world.setGameRule(GameRules.LOCATOR_BAR, false);
	}

	private void teleportPlayers(Match match, Location spawn) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.teleport(spawn);
			}
		}
	}

	private void setMatchRespawnPoint(Match match, Location spawn) {
		if (match == null || spawn == null) {
			return;
		}
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.setRespawnLocation(spawn, true);
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
			if (match.getState() != MatchState.WORLD_CREATING || worlds == null) {
				stopWorldProgressTask(match.getId());
				return;
			}
			int ready = countReady(worlds);
			String message = "<yellow>⌛</yellow> Đang tạo thế giới: <white>" + ready + "/" + WORLD_TOTAL + "</white>";
			sendActionBar(match, message);
		}, 0L, 20L);
		worldProgressTasks.put(match.getId(), task);
	}

	private void stopWorldProgressTask(String matchId) {
		stopTask(worldProgressTasks, matchId);
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
		sendActionBar(match, "<yellow>⌛</yellow> Đang chuẩn bị Overworld: <white>0/" + total + "</white> <gray>(0%)</gray>");

		BukkitRunnable runnable = new BukkitRunnable() {
			int index = 0;

			@Override
			public void run() {
				if (match.getState() != MatchState.WORLD_READY) {
					stopOverworldWarmupTask(match.getId());
					cancel();
					return;
				}
				int batch = 2;
				for (int i = 0; i < batch && index < total; i++) {
					int[] coord = chunks.get(index++);
					overworld.getChunkAt(coord[0], coord[1]);
				}
				int percent = (index * 100) / total;
				String message = "<yellow>⌛</yellow> Đang chuẩn bị Overworld: <white>" + index + "/" + total + "</white> <gray>(" + percent + "%)</gray>";
				sendActionBar(match, message);
				if (index >= total) {
					stopOverworldWarmupTask(match.getId());
					cancel();
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
		broadcast(match, "<yellow>⌛</yellow> Bắt đầu đếm ngược <white>" + COUNTDOWN_SECONDS + "</white> giây.");
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
				showCountdownSubtitle(match, seconds);
				playCountdownSound(match, seconds);
				seconds--;
			}
		};
		BukkitTask task = runnable.runTaskTimer(plugin, 0L, 20L);
		countdownTasks.put(match.getId(), task);
	}

	private void stopCountdownTask(String matchId) {
		stopTask(countdownTasks, matchId);
	}

	private void prepareRunPending(Match match, World overworld) {
		if (match == null || overworld == null) {
			return;
		}
		Location spawn = overworld.getSpawnLocation();
		applyWorldBorder(overworld, spawn, match.getSettings());
		teleportPlayers(match, spawn);
		setMatchRespawnPoint(match, spawn);
		clearLobbyItems(match);
		setGameMode(match, GameMode.ADVENTURE);
		giveSpeedrunnerKit(match, match.getSpeedrunnerId());
		match.setState(MatchState.RUN_PENDING);
		broadcastMatchInfo(match, overworld);
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
		Text.tell(player, "<gray>+--------------------------------------------------+</gray>");
		Text.tell(player, " <yellow>ℹ</yellow> <b><u>MANHUNT</u></b> <gray>•</gray> <white>Hướng dẫn nhanh</white>");
		Text.tell(player, "<gray>+--------------------------------------------------+</gray>");
		Text.tell(player, " <white>● Mục tiêu:</white> <#8fd3ff><b>Speedrunner</b></#8fd3ff> <white>hạ <b>Ender Dragon</b>.</white>");
		Text.tell(player, " <white>● Điều kiện thắng:</white> <#ffb84d><b>Hunters</b></#ffb84d> <white>giết <b>Speedrunner</b>.</white>");
		Text.tell(player, " <white>● Bắt đầu:</white> <white>Speedrunner đánh <b>một</b> Hunter.</white>");
		Text.tell(player, " <white>● Khi bắt đầu:</white> <white>Hunters nhận <b>la bàn truy vết</b>.</white>");
		if (isSpeedrunner) {
			Text.tell(player, " <#8fd3ff>● Vai trò của bạn:</#8fd3ff> <white><b>kết liễu rồng</b> và sinh tồn.</white>");
		} else {
			Text.tell(player, " <#ffb84d>● Vai trò của bạn:</#ffb84d> <white><b>hạ Speedrunner</b> bằng truy đuổi.</white>");
		}
		Text.tell(player, "<gray>+--------------------------------------------------+</gray>");
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
					? "<yellow>ℹ</yellow> <white>Bạn là Speedrunner</white> <gray>•</gray> <white>Hãy đánh một Hunter để bắt đầu</white>"
					: "<yellow>ℹ</yellow> <white>Chờ Speedrunner đánh một Hunter để bắt đầu trận đấu</white>";
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
			String timeText = formatRunTime(System.currentTimeMillis() - startedAt);
			String message = "<#32faa7>⌚</#32faa7> <white>" + timeText + "</white>";
			sendActionBar(match, message);
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
				player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
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
