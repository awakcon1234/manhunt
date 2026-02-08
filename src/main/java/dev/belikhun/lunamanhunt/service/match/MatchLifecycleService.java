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
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
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
	private final Random random = new Random();
	private final ConcurrentMap<String, BukkitTask> worldProgressTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> countdownTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> pendingActionTasks = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, BukkitTask> runningActionTasks = new ConcurrentHashMap<>();

	public MatchLifecycleService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager,
			CompassService compassService, SpeedrunnerKitService kitService, CardService cardService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
		this.kitService = Objects.requireNonNull(kitService, "kitService");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
	}

	public void handleWorldsCreating(Match match) {
		if (match == null || match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
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
		String label = formatWorldLabel(bukkitWorld.getEnvironment());
		String seed = String.valueOf(bukkitWorld.getSeed());
		broadcast(match, "<green>✔</green> Thế giới <yellow>" + label + "</yellow> đã tạo xong. Seed: <white>" + seed + "</white>.");
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
		startCountdown(match, overworld);
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
		return match.getHunters().contains(attacker.getUniqueId())
			&& match.getHunters().contains(victim.getUniqueId());
	}

	private void markMatchFailed(Match match, String message) {
		match.setState(MatchState.FINISHED);
		match.setEndedAtMillis(System.currentTimeMillis());
		stopWorldProgressTask(match.getId());
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

	private void teleportPlayers(Match match, Location spawn) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.teleport(spawn);
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
			String current = resolveCurrentWorldLabel(worlds);
			int percent = (ready * 100) / WORLD_TOTAL;
			String message = "<yellow>⌛</yellow> Đang tạo thế giới: <white>" + current + "</white> <gray>(" + percent + "%)</gray>";
			sendActionBar(match, message);
		}, 0L, 20L);
		worldProgressTasks.put(match.getId(), task);
	}

	private void stopWorldProgressTask(String matchId) {
		stopTask(worldProgressTasks, matchId);
	}

	private void startCountdown(Match match, World overworld) {
		stopCountdownTask(match.getId());
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
		clearLobbyItems(match);
		setGameMode(match, GameMode.ADVENTURE);
		giveSpeedrunnerKit(match, match.getSpeedrunnerId());
		match.setState(MatchState.RUN_PENDING);
		broadcastMatchInfo(match, overworld);
		startPendingActionTask(match);
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
				String message = (speedrunnerId != null && speedrunnerId.equals(id))
					? "<yellow>⚔</yellow> <white>Đánh một Hunter để bắt đầu trận đấu!</white>"
					: "<yellow>⚔</yellow> <white>Trận đấu sẽ bắt đầu khi Speedrunner đánh một Hunter...</white>";
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
		Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ZERO);
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.showTitle(Title.title(net.kyori.adventure.text.Component.empty(),
					Text.mm("<yellow>" + seconds + "</yellow>"), times));
			}
		}
	}

	private void playCountdownSound(Match match, int seconds) {
		Sound sound = seconds <= 3 ? Sound.BLOCK_NOTE_BLOCK_PLING : Sound.BLOCK_NOTE_BLOCK_HAT;
		float pitch = seconds <= 3 ? 1.6f : 1.0f;
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.playSound(player.getLocation(), sound, 1.0f, pitch);
			}
		}
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
		String seed = String.valueOf(overworld.getSeed());
		broadcast(match, "<yellow>ℹ</yellow> Thông tin trận đấu:");
		broadcast(match, "<gray>● Mã phòng: <white>" + match.getId() + "</white></gray>");
		broadcast(match, "<gray>● Seed: <white>" + seed + "</white></gray>");
		broadcast(match, "<gray>● Viền ban đầu: <white>" + match.getSettings().getStartingBorder() + "</white></gray>");
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

	private String resolveCurrentWorldLabel(MatchWorlds worlds) {
		if (!worlds.isOverworldReady()) {
			return "Overworld";
		}
		if (!worlds.isNetherReady()) {
			return "Nether";
		}
		if (!worlds.isEndReady()) {
			return "The End";
		}
		return "Hoàn tất";
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
}
