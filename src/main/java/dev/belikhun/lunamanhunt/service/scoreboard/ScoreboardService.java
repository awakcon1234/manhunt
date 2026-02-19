package dev.belikhun.lunamanhunt.service.scoreboard;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.profile.MatchHistoryEntry;
import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.card.ActiveCard;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.util.Debug;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.exception.NoPacketAdapterAvailableException;
import net.megavex.scoreboardlibrary.api.noop.NoopScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ScoreboardService {
	private static final MiniMessage MINI = MiniMessage.miniMessage();
	private static final int PLACEHOLDER_REFRESH_MS = 5000;
	private static final int DEFAULT_UPDATE_TICKS = 10;
	private static final int ACTIVE_LINES_MAX = 4;
	private static final int HISTORY_LINES_MAX = 3;
	private static final int PROFILE_CACHE_MS = 5000;
	private static final int DEBUG_LOG_INTERVAL_MS = 5000;
	private static final int DEBUG_TICK_LOG_INTERVAL_MS = 10000;
	private static final String TEMPLATE_LOBBY = "lobby";
	private static final String TEMPLATE_MATCH_LOBBY = "match_lobby";
	private static final String TEMPLATE_PREPARING = "match_preparing";
	private static final String TEMPLATE_COUNTDOWN = "match_countdown";
	private static final String TEMPLATE_WAITING_START = "match_waiting_start";
	private static final String TEMPLATE_PLAYING_SPEEDRUNNER = "match_playing_speedrunner";
	private static final String TEMPLATE_PLAYING_HUNTER = "match_playing_hunter";
	private static final String TEMPLATE_FINISHED = "match_finished";

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final CardService cardService;
	private final MatchLifecycleService lifecycleService;
	private final PointService pointService;
	private final ProfileStore profileStore;
	private final Map<UUID, PlayerEntry> players = new HashMap<>();
	private final Set<UUID> onlineIdsTmp = new HashSet<>();
	private final Map<UUID, ProfileSnapshot> profileCache = new HashMap<>();
	private final Map<UUID, Long> debugNextLogMillis = new HashMap<>();
	private ScoreboardLibrary library;
	private int taskId = -1;
	private boolean placeholderApiEnabled = false;
	private long placeholderApiNextRefreshMs = 0L;
	private boolean enabled = true;
	private int updatePeriodTicks = DEFAULT_UPDATE_TICKS;
	private boolean debugEnabled = false;
	private long debugNextTickLogMillis = 0L;

	private static final class PlayerEntry {
		private Sidebar sidebar;
		private int lastLineCount;
		private int lastEntityId;
	}

	public ScoreboardService(JavaPlugin plugin, LobbyManager lobbyManager, CardService cardService,
			MatchLifecycleService lifecycleService, PointService pointService, ProfileStore profileStore) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
		this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
	}

	public void start() {
		if (taskId != -1) {
			return;
		}
		this.enabled = cfgBool("scoreboard.enabled", true);
		this.debugEnabled = Debug.isEnabled();
		if (!enabled) {
			return;
		}
		if (!debugEnabled) {
			debugNextLogMillis.clear();
		}
		if (Debug.isEnabled()) {
			plugin.getLogger().info("[ScoreboardDebug] enabled=" + enabled
				+ " updateTicks=" + updatePeriodTicks);
		}
		try {
			library = ScoreboardLibrary.loadScoreboardLibrary(plugin);
		} catch (NoPacketAdapterAvailableException e) {
			library = new NoopScoreboardLibrary();
			plugin.getLogger().warning("ScoreboardLibrary khong hoat dong: " + e.getMessage());
		} catch (Throwable t) {
			library = new NoopScoreboardLibrary();
			plugin.getLogger().warning("Khong the khoi tao ScoreboardLibrary: " + t.getMessage());
		}
		updatePeriodTicks = Math.max(1, cfgInt("scoreboard.update-ticks", DEFAULT_UPDATE_TICKS));
		taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 0L, updatePeriodTicks).getTaskId();
	}

	public void stop() {
		if (taskId != -1) {
			try {
				Bukkit.getScheduler().cancelTask(taskId);
			} catch (Throwable ignored) {
			}
			taskId = -1;
		}
		for (PlayerEntry entry : players.values()) {
			try {
				if (entry != null && entry.sidebar != null) {
					entry.sidebar.close();
				}
			} catch (Throwable ignored) {
			}
		}
		players.clear();
		if (library != null) {
			try {
				library.close();
			} catch (Throwable ignored) {
			}
		}
		library = null;
	}

	private void tick() {
		if (!enabled) {
			return;
		}
		refreshPlaceholderApiEnabledIfNeeded(System.currentTimeMillis());
		cleanupOfflinePlayers();
		logTickDebug();

		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				tickPlayer(player);
			} catch (Throwable t) {
				if (debugEnabled) {
					plugin.getLogger().warning("[ScoreboardDebug] tickPlayer failed: player="
						+ (player == null ? "?" : player.getName()) + " error=" + t.getClass().getSimpleName());
				}
			}
		}
	}

	private void logTickDebug() {
		if (!debugEnabled) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now < debugNextTickLogMillis) {
			return;
		}
		debugNextTickLogMillis = now + DEBUG_TICK_LOG_INTERVAL_MS;
		plugin.getLogger().info("[ScoreboardDebug] tick online=" + Bukkit.getOnlinePlayers().size());
	}

	private void tickPlayer(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		String template = resolveTemplate(player, match);
		Map<String, String> placeholders = buildPlaceholders(player, match);
		Component title = parse(player, cfgString("scoreboard.templates." + template + ".title", ""), placeholders);
		java.util.List<Component> lines;
		if (TEMPLATE_LOBBY.equals(template)) {
			lines = buildLobbyLines(player, placeholders);
		} else if (TEMPLATE_PLAYING_SPEEDRUNNER.equals(template) || TEMPLATE_PLAYING_HUNTER.equals(template)) {
			lines = buildRunningLines(player, match, template, placeholders);
		} else {
			lines = parseLines(player,
				cfgStringList("scoreboard.templates." + template + ".lines", java.util.List.of()),
				placeholders);
		}
		Sidebar sidebar = ensureSidebar(player);
		applySidebarComponents(player, sidebar, title, lines);
	}

	private String resolveTemplate(Player player, Match match) {
		if (match == null) {
			return TEMPLATE_LOBBY;
		}
		MatchState state = match.getState();
		if (state == MatchState.LOBBY) {
			return TEMPLATE_MATCH_LOBBY;
		}
		if (state == MatchState.WORLD_CREATING || state == MatchState.WORLD_READY) {
			int countdown = lifecycleService.getCountdownRemainingSeconds(match.getId());
			if (countdown > 0) {
				return TEMPLATE_COUNTDOWN;
			}
			return TEMPLATE_PREPARING;
		}
		if (state == MatchState.RUN_PENDING) {
			return TEMPLATE_WAITING_START;
		}
		if (state == MatchState.RUNNING) {
			if (isSpeedrunner(player, match)) {
				return TEMPLATE_PLAYING_SPEEDRUNNER;
			}
			if (player != null && match.getHunters().contains(player.getUniqueId())) {
				return TEMPLATE_PLAYING_HUNTER;
			}
			return TEMPLATE_PLAYING_HUNTER;
		}
		if (state == MatchState.ENDING || state == MatchState.FINISHED || state == MatchState.CLEANUP) {
			return TEMPLATE_FINISHED;
		}
		return TEMPLATE_LOBBY;
	}

	private Map<String, String> buildPlaceholders(Player player, Match match) {
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("player_name", safeStr(player.getName()));
		placeholders.put("player_display", resolvePlayerDisplay(player, match));
		placeholders.put("role_display", resolveRoleDisplay(player, match));
		placeholders.put("role_chip", formatRoleChip(player, match));

		if (match == null) {
			placeholders.put("match_id", "-");
			placeholders.put("match_state", "-");
			placeholders.put("match_state_display", "-");
			placeholders.put("match_owner", "-");
			placeholders.put("match_players", "0");
			placeholders.put("match_hunters", "0");
			placeholders.put("match_speedrunner", "-");
			placeholders.put("match_points", "-");
			placeholders.put("match_timer", "00:00.000");
			placeholders.put("match_countdown", "-");
			placeholders.put("match_winner_display", "-");
			placeholders.put("match_duration", "-");
			placeholders.put("match_world_stage", "-");
			placeholders.put("match_world_progress", "0");
			placeholders.put("match_world_eta", "--:--s");
			placeholders.put("match_world_ready", "0");
			placeholders.put("match_world_total", "3");
			placeholders.put("match_world_seed", "-");
			placeholders.put("match_pending_hint", "-");
			placeholders.put("match_stats_hunters_killed", "0");
			placeholders.put("match_stats_mob_kills", "0");
			placeholders.put("match_stats_natural_deaths", "0");
			placeholders.put("match_stats_cards_used", "0");
			placeholders.put("match_stats_damage_hunters", "0");
			placeholders.put("match_time_to_nether", "-");
			placeholders.put("match_time_to_end", "-");
			placeholders.put("match_kills", "0");
			placeholders.put("match_setting_time", "-");
			placeholders.put("match_setting_time_display", "-");
			placeholders.put("match_setting_weather", "-");
			placeholders.put("match_setting_weather_display", "-");
			placeholders.put("match_setting_difficulty", "-");
			placeholders.put("match_setting_difficulty_display", "-");
			placeholders.put("match_setting_keep_inventory", "-");
			placeholders.put("match_setting_keep_inventory_display", "-");
			placeholders.put("match_setting_border_limit", "-");
			placeholders.put("match_setting_starting_border", "-");
			placeholders.put("match_setting_visibility", "-");
			placeholders.put("match_setting_visibility_display", "-");
			placeholders.put("match_setting_world_seed", "-");
			placeholders.put("match_setting_world_version", "-");
			placeholders.put("match_setting_kit_id", "-");
			placeholders.put("match_setting_speedrunner_selection", "-");
			placeholders.put("match_id_chip", UiColors.matchIdChip("-"));
			placeholders.put("match_owner_chip", "<gray>👑 -</gray>");
			placeholders.put("match_players_chip", "<gray>👻 0</gray>");
			placeholders.put("match_hunters_chip", "<gray>👻 0</gray>");
			placeholders.put("match_timer_chip", "<gray>⌚ --:--</gray>");
			placeholders.put("match_points_chip", "<gray>🔥 -</gray>");
			placeholders.put("match_kills_chip", "<gray>⚔ 0</gray>");
			placeholders.put("match_countdown_chip", "<gray>⏳ -</gray>");
			placeholders.put("match_world_progress_chip", "<gray>0%</gray>");
			placeholders.put("match_world_progress_bar", "<dark_gray>░░░░░░░░░░░░</dark_gray>");
			placeholders.put("match_world_progress_bar_chip", "<white>▏</white> <dark_gray>░░░░░░░░░░░░</dark_gray> <gray>0%</gray>");
			placeholders.put("match_world_eta_chip", "<gray>⌚ --:--s</gray>");
			placeholders.put("match_visibility_chip", "<gray>🔒 -</gray>");
			placeholders.put("match_difficulty_chip", "<gray>⚔ -</gray>");
			placeholders.put("match_border_chip", "<gray>⌂ -</gray>");
			placeholders.put("match_start_border_chip", "<gray>◎ -</gray>");
			placeholders.put("match_winner_chip", "<gray>★ -</gray>");
			placeholders.put("profile_playtime_chip", "<gray>⌚ -</gray>");
			placeholders.put("profile_best_time_chip", "<gray>★ -</gray>");
			placeholders.put("profile_kills_chip", "<gray>⚔ 0</gray>");
			placeholders.put("profile_hunter_chip", "<gray>👻 0/0</gray>");
			placeholders.put("profile_speedrunner_chip", "<gray>🏹 0/0</gray>");
			fillProfilePlaceholders(placeholders, player.getUniqueId());
			fillProfileChipPlaceholders(placeholders);
			return placeholders;
		}

		placeholders.put("match_id", safeStr(match.getId()));
		placeholders.put("match_state", match.getState() == null ? "-" : match.getState().name());
		placeholders.put("match_state_display", resolveMatchStateDisplay(match.getState()));
		placeholders.put("match_owner", resolvePlayerName(match.getOwnerId()));
		placeholders.put("match_players", String.valueOf(match.getLobbyPlayers().size()));
		placeholders.put("match_hunters", String.valueOf(match.getHunters().size()));
		placeholders.put("match_speedrunner", resolvePlayerName(match.getSpeedrunnerId()));

		String points = isSpeedrunner(player, match)
			? pointService.formatPoints(match.getSpeedrunnerPoints())
			: "-";
		placeholders.put("match_points", points);

		placeholders.put("match_timer", formatRunTimer(match));
		placeholders.put("match_duration", formatMatchDuration(match));
		placeholders.put("match_winner_display", resolveWinnerDisplay(match.getWinner()));

		int countdown = lifecycleService.getCountdownRemainingSeconds(match.getId());
		placeholders.put("match_countdown", countdown > 0 ? String.valueOf(countdown) : "-");

		MatchLifecycleService.WorldProgressSnapshot snapshot = lifecycleService.getWorldProgressSnapshot(match.getId());
		if (snapshot != null) {
			placeholders.put("match_world_stage", safeStr(snapshot.getStageLabel()));
			placeholders.put("match_world_progress", String.valueOf(snapshot.getPercent()));
			placeholders.put("match_world_eta", formatEta(snapshot.getEtaSeconds()));
			placeholders.put("match_world_ready", String.valueOf(snapshot.getReadyWorlds()));
			placeholders.put("match_world_total", String.valueOf(snapshot.getTotalWorlds()));
		} else {
			placeholders.put("match_world_stage", "-");
			placeholders.put("match_world_progress", "0");
			placeholders.put("match_world_eta", "--:--s");
			placeholders.put("match_world_ready", "0");
			placeholders.put("match_world_total", "3");
		}

		placeholders.put("match_pending_hint", resolvePendingHint(player, match));
		placeholders.put("match_world_seed", resolveSeed(match));

		var stats = match.getStats();
		placeholders.put("match_stats_hunters_killed", String.valueOf(stats.getHuntersKilled()));
		placeholders.put("match_stats_mob_kills", String.valueOf(stats.getMobKills()));
		placeholders.put("match_stats_natural_deaths", String.valueOf(stats.getNaturalDeaths()));
		placeholders.put("match_stats_cards_used", String.valueOf(stats.getCardsUsed()));
		placeholders.put("match_stats_damage_hunters", formatDecimal(stats.getDamageToHunters(), 1));
		placeholders.put("match_time_to_nether", formatMilestone(stats.getTimeToNetherMillis()));
		placeholders.put("match_time_to_end", formatMilestone(stats.getTimeToEndMillis()));
		placeholders.put("match_kills", String.valueOf(stats.getHuntersKilled()));

		var settings = match.getSettings();
		if (settings != null) {
			placeholders.put("match_setting_time", settings.getWorldTime() == null ? "-" : settings.getWorldTime().name());
			placeholders.put("match_setting_time_display", formatWorldTime(settings.getWorldTime()));
			placeholders.put("match_setting_weather", settings.getWorldWeather() == null ? "-" : settings.getWorldWeather().name());
			placeholders.put("match_setting_weather_display", formatWorldWeather(settings.getWorldWeather()));
			placeholders.put("match_setting_difficulty", settings.getWorldDifficulty() == null ? "-" : settings.getWorldDifficulty().name());
			placeholders.put("match_setting_difficulty_display", formatWorldDifficulty(settings.getWorldDifficulty()));
			placeholders.put("match_setting_keep_inventory", String.valueOf(settings.isKeepInventory()));
			placeholders.put("match_setting_keep_inventory_display", settings.isKeepInventory() ? "Bật" : "Tắt");
			placeholders.put("match_setting_border_limit", String.valueOf(settings.getWorldBorderLimit()));
			placeholders.put("match_setting_starting_border", String.valueOf(settings.getStartingBorder()));
			placeholders.put("match_setting_visibility", settings.getVisibility() == null ? "-" : settings.getVisibility().name());
			placeholders.put("match_setting_visibility_display", formatVisibility(settings.getVisibility()));
			placeholders.put("match_setting_world_seed", safeStr(settings.getWorldSeed()));
			placeholders.put("match_setting_world_version", safeStr(settings.getWorldGenerationVersion()));
			placeholders.put("match_setting_kit_id", safeStr(settings.getSpeedrunnerKitId()));
			placeholders.put("match_setting_speedrunner_selection",
				settings.getSpeedrunnerSelection() == null ? "-" : settings.getSpeedrunnerSelection().name());
		} else {
			placeholders.put("match_setting_time", "-");
			placeholders.put("match_setting_time_display", "-");
			placeholders.put("match_setting_weather", "-");
			placeholders.put("match_setting_weather_display", "-");
			placeholders.put("match_setting_difficulty", "-");
			placeholders.put("match_setting_difficulty_display", "-");
			placeholders.put("match_setting_keep_inventory", "-");
			placeholders.put("match_setting_keep_inventory_display", "-");
			placeholders.put("match_setting_border_limit", "-");
			placeholders.put("match_setting_starting_border", "-");
			placeholders.put("match_setting_visibility", "-");
			placeholders.put("match_setting_visibility_display", "-");
			placeholders.put("match_setting_world_seed", "-");
			placeholders.put("match_setting_world_version", "-");
			placeholders.put("match_setting_kit_id", "-");
			placeholders.put("match_setting_speedrunner_selection", "-");
		}
		fillProfilePlaceholders(placeholders, player.getUniqueId());
		fillProfileChipPlaceholders(placeholders);
		fillMatchChipPlaceholders(placeholders, player, match);

		return placeholders;
	}

	private void fillProfileChipPlaceholders(Map<String, String> placeholders) {
		if (placeholders == null) {
			return;
		}
		placeholders.put("profile_playtime_chip", "<#32faa7>⌚</#32faa7> <white>"
			+ safeStr(placeholders.get("profile_playtime")) + "</white>");
		placeholders.put("profile_best_time_chip", "<#ffc012>★</#ffc012> <white>"
			+ safeStr(placeholders.get("profile_best_time")) + "</white>");
		placeholders.put("profile_kills_chip", "<#ff9f68>⚔</#ff9f68> <white>"
			+ safeStr(placeholders.get("profile_kills")) + "</white>");
		placeholders.put("profile_hunter_chip", "<aqua>👻</aqua> <white>"
			+ safeStr(placeholders.get("profile_hunter_wins")) + "</white>/<white>"
			+ safeStr(placeholders.get("profile_hunter_total")) + "</white>");
		placeholders.put("profile_speedrunner_chip", "<aqua>🏹</aqua> <white>"
			+ safeStr(placeholders.get("profile_speedrunner_wins")) + "</white>/<white>"
			+ safeStr(placeholders.get("profile_speedrunner_total")) + "</white>");
	}

	private void fillMatchChipPlaceholders(Map<String, String> placeholders, Player player, Match match) {
		if (placeholders == null) {
			return;
		}
		String matchId = safeStr(placeholders.get("match_id"));
		placeholders.put("match_id_chip", UiColors.matchIdChip(matchId));
		placeholders.put("match_owner_chip", "<yellow>👑</yellow> <white>"
			+ safeStr(placeholders.get("match_owner")) + "</white>");
		placeholders.put("match_players_chip", "<aqua>👻</aqua> <white>"
			+ safeStr(placeholders.get("match_players")) + "</white>");
		placeholders.put("match_hunters_chip", "<aqua>👻</aqua> <white>"
			+ safeStr(placeholders.get("match_hunters")) + "</white>");
		placeholders.put("match_timer_chip", "<#32faa7>⌚</#32faa7> <white>"
			+ safeStr(placeholders.get("match_timer")) + "</white>");
		placeholders.put("match_points_chip", "<#ffc012>🔥</#ffc012> <white>"
			+ safeStr(placeholders.get("match_points")) + "</white>");
		placeholders.put("match_kills_chip", "<#ff9f68>⚔</#ff9f68> <white>"
			+ safeStr(placeholders.get("match_kills")) + "</white>");
		placeholders.put("match_countdown_chip", "<#e7ffa3>⏳</#e7ffa3> <white>"
			+ safeStr(placeholders.get("match_countdown")) + "</white>");
		int worldProgressPercent = safeInt(placeholders.get("match_world_progress"), 0);
		String worldProgressBar = formatProgressBar(worldProgressPercent, 12);
		placeholders.put("match_world_progress_chip", "<white>" + worldProgressPercent + "%</white>");
		placeholders.put("match_world_progress_bar", worldProgressBar);
		placeholders.put("match_world_progress_bar_chip", "<white>▏</white> "
			+ worldProgressBar + " <white>" + worldProgressPercent + "%</white>");
		placeholders.put("match_world_eta_chip", "<#32faa7>⌚</#32faa7> <white>"
			+ safeStr(placeholders.get("match_world_eta")) + "</white>");
		placeholders.put("match_visibility_chip", "<gray>🔒</gray> <white>"
			+ safeStr(placeholders.get("match_setting_visibility_display")) + "</white>");
		placeholders.put("match_difficulty_chip", "<gray>⚔</gray> <white>"
			+ safeStr(placeholders.get("match_setting_difficulty_display")) + "</white>");
		placeholders.put("match_border_chip", "<gray>⌂</gray> <white>"
			+ safeStr(placeholders.get("match_setting_border_limit")) + "</white>");
		placeholders.put("match_start_border_chip", "<gray>◎</gray> <white>"
			+ safeStr(placeholders.get("match_setting_starting_border")) + "</white>");
		placeholders.put("match_winner_chip", "<gold>★</gold> "
			+ safeStr(placeholders.get("match_winner_display")));
		placeholders.put("role_chip", formatRoleChip(player, match));
	}

	private String formatRoleChip(Player player, Match match) {
		if (match == null || player == null) {
			return "<gray>● Người chơi</gray>";
		}
		if (isSpeedrunner(player, match)) {
			return "<gold>★</gold> " + UiColors.roleSpeedrunner();
		}
		if (match.getHunters().contains(player.getUniqueId())) {
			return "<red>⚔</red> " + UiColors.roleHunter();
		}
		return "<gray>● Người chơi</gray>";
	}

	private java.util.List<Component> buildRunningLines(Player player, Match match, String template,
			Map<String, String> placeholders) {
		java.util.List<Component> lines = new java.util.ArrayList<>();
		java.util.List<String> header = cfgStringList("scoreboard.templates." + template + ".header", java.util.List.of());
		java.util.List<String> footer = cfgStringList("scoreboard.templates." + template + ".footer", java.util.List.of());
		lines.addAll(parseLines(player, header, placeholders));

		if (TEMPLATE_PLAYING_SPEEDRUNNER.equals(template)) {
			String powerupLine = cfgString("scoreboard.templates." + template + ".powerup_line", "");
			String effectLine = cfgString("scoreboard.templates." + template + ".effect_line", "");
			String powerupEmpty = cfgString("scoreboard.templates." + template + ".powerup_empty", "<gray>Không có</gray>");
			String effectEmpty = cfgString("scoreboard.templates." + template + ".effect_empty", "<gray>Không có</gray>");
			java.util.List<String> middle = cfgStringList("scoreboard.templates." + template + ".middle", java.util.List.of());
			java.util.List<Component> powerupLines = buildActiveLines(player, match, placeholders, CardType.POWER_UP,
				powerupLine, powerupEmpty);
			java.util.List<Component> effectLines = buildActiveLines(player, match, placeholders, CardType.EFFECT,
				effectLine, effectEmpty);
			lines.addAll(powerupLines);
			lines.addAll(parseLines(player, middle, placeholders));
			lines.addAll(effectLines);
		} else {
			String curseLine = cfgString("scoreboard.templates." + template + ".curse_line", "");
			String curseEmpty = cfgString("scoreboard.templates." + template + ".curse_empty", "<gray>Không có</gray>");
			lines.addAll(buildActiveLines(player, match, placeholders, CardType.CURSE, curseLine, curseEmpty));
		}

		lines.addAll(parseLines(player, footer, placeholders));
		return lines;
	}

	private java.util.List<Component> buildActiveLines(Player player, Match match, Map<String, String> basePlaceholders,
			CardType type, String lineTemplate, String emptyLine) {
		if (lineTemplate == null || lineTemplate.isBlank()) {
			return java.util.List.of();
		}
		java.util.List<ActiveCard> active = cardService.getActiveCards(match);
		if (active == null || active.isEmpty()) {
			return java.util.List.of(parse(player, emptyLine, basePlaceholders));
		}
		java.util.List<Component> out = new java.util.ArrayList<>();
		long now = System.currentTimeMillis();
		for (ActiveCard entry : active) {
			if (entry == null || entry.card() == null || entry.card().getType() == null) {
				continue;
			}
			if (entry.card().getType() != type) {
				continue;
			}
			Map<String, String> placeholders = new HashMap<>(basePlaceholders);
			String name = safeStr(entry.card().getName());
			String time = "∞";
			if (entry.card().isTimed()) {
				long remainMs = Math.max(0L, entry.expiresAtMillis() - now);
				long remainSeconds = Math.round(remainMs / 1000.0);
				time = formatShortDuration(remainSeconds);
			}
			placeholders.put("card_name", name);
			placeholders.put("card_time", time);
			out.add(parse(player, lineTemplate, placeholders));
			if (out.size() >= ACTIVE_LINES_MAX) {
				break;
			}
		}
		if (out.isEmpty()) {
			return java.util.List.of(parse(player, emptyLine, basePlaceholders));
		}
		return out;
	}

	private void fillProfilePlaceholders(Map<String, String> placeholders, UUID playerId) {
		if (placeholders == null || playerId == null) {
			return;
		}
		ProfileSnapshot snapshot = getProfileSnapshot(playerId);
		placeholders.put("profile_playtime", snapshot.playtimeDisplay);
		placeholders.put("profile_best_time", snapshot.bestTimeDisplay);
		placeholders.put("profile_kills", String.valueOf(snapshot.kills));
		placeholders.put("profile_hunter_wins", String.valueOf(snapshot.hunterWins));
		placeholders.put("profile_hunter_total", String.valueOf(snapshot.hunterTotal));
		placeholders.put("profile_hunter_winrate", snapshot.hunterWinrate);
		placeholders.put("profile_speedrunner_wins", String.valueOf(snapshot.speedrunnerWins));
		placeholders.put("profile_speedrunner_total", String.valueOf(snapshot.speedrunnerTotal));
		placeholders.put("profile_speedrunner_winrate", snapshot.speedrunnerWinrate);
	}

	private java.util.List<Component> buildLobbyLines(Player player, Map<String, String> placeholders) {
		String matchLine = cfgString("scoreboard.templates.lobby.match_line", "");
		if (matchLine == null || matchLine.isBlank()) {
			return parseLines(player,
				cfgStringList("scoreboard.templates.lobby.lines", java.util.List.of()),
				placeholders);
		}
		java.util.List<Component> lines = new java.util.ArrayList<>();
		java.util.List<String> header = cfgStringList("scoreboard.templates.lobby.header", java.util.List.of());
		java.util.List<String> footer = cfgStringList("scoreboard.templates.lobby.footer", java.util.List.of());
		lines.addAll(parseLines(player, header, placeholders));

		ProfileSnapshot snapshot = getProfileSnapshot(player.getUniqueId());
		if (snapshot.recentMatches.isEmpty()) {
			String empty = cfgString("scoreboard.templates.lobby.match_empty", "<gray>Chưa có trận gần đây</gray>");
			lines.add(parse(player, empty, placeholders));
		} else {
			int index = 1;
			for (HistoryLine entry : snapshot.recentMatches) {
				Map<String, String> linePlaceholders = new HashMap<>(placeholders);
				linePlaceholders.put("history_index", String.valueOf(index));
				linePlaceholders.put("history_match_id", safeStr(entry.matchId));
				linePlaceholders.put("history_match_id_chip", UiColors.matchIdText(entry.matchId));
				linePlaceholders.put("history_role_display", entry.roleDisplay);
				linePlaceholders.put("history_outcome", entry.outcomeDisplay);
				linePlaceholders.put("history_duration", entry.durationDisplay);
				linePlaceholders.put("history_time_ago", entry.timeAgo);
				lines.add(parse(player, matchLine, linePlaceholders));
				index++;
			}
		}

		lines.addAll(parseLines(player, footer, placeholders));
		return lines;
	}

	private ProfileSnapshot getProfileSnapshot(UUID playerId) {
		long now = System.currentTimeMillis();
		ProfileSnapshot cached = profileCache.get(playerId);
		if (cached != null && now - cached.loadedAtMillis < PROFILE_CACHE_MS) {
			return cached;
		}
		PlayerProfile profile;
		try {
			profile = profileStore.load(playerId);
		} catch (Exception ignored) {
			profile = new PlayerProfile(playerId);
		}
		ProfileSnapshot snapshot = buildProfileSnapshot(profile, now);
		profileCache.put(playerId, snapshot);
		return snapshot;
	}

	private ProfileSnapshot buildProfileSnapshot(PlayerProfile profile, long nowMillis) {
		long playtime = 0L;
		int hunterWins = 0;
		int hunterTotal = 0;
		int speedrunnerWins = 0;
		int speedrunnerTotal = 0;
		int kills = 0;
		java.util.List<HistoryLine> recent = new java.util.ArrayList<>();
		if (profile != null) {
			for (MatchHistoryEntry entry : profile.getMatchHistory()) {
				playtime += Math.max(0L, entry.getDurationMillis());
				if ("HUNTER".equalsIgnoreCase(entry.getRole())) {
					hunterTotal++;
					if (entry.isWon()) {
						hunterWins++;
					}
				} else if ("SPEEDRUNNER".equalsIgnoreCase(entry.getRole())) {
					speedrunnerTotal++;
					if (entry.isWon()) {
						speedrunnerWins++;
					}
					kills += Math.max(0, entry.getHuntersKilled());
				}
			}
			for (int i = profile.getMatchHistory().size() - 1; i >= 0 && recent.size() < HISTORY_LINES_MAX; i--) {
				MatchHistoryEntry entry = profile.getMatchHistory().get(i);
				recent.add(toHistoryLine(entry, nowMillis));
			}
		}
		String playtimeDisplay = formatPlaytime(playtime);
		String bestTimeDisplay = formatDuration(profile == null ? 0L : profile.getBestTimeMillis());
		String hunterWinrate = formatWinrate(hunterWins, hunterTotal);
		String speedrunnerWinrate = formatWinrate(speedrunnerWins, speedrunnerTotal);
		return new ProfileSnapshot(nowMillis, playtimeDisplay, bestTimeDisplay, kills,
			hunterWins, hunterTotal, hunterWinrate, speedrunnerWins, speedrunnerTotal, speedrunnerWinrate, recent);
	}

	private HistoryLine toHistoryLine(MatchHistoryEntry entry, long nowMillis) {
		String matchId = safeStr(entry.getMatchId());
		String roleDisplay = formatHistoryRole(entry.getRole());
		String outcomeDisplay = formatHistoryOutcome(entry);
		String durationDisplay = formatDuration(entry.getDurationMillis());
		long timeMillis = entry.getEndedAtMillis() > 0 ? entry.getEndedAtMillis() : entry.getStartedAtMillis();
		String timeAgo = formatRelativeTime(nowMillis, timeMillis);
		return new HistoryLine(matchId, roleDisplay, outcomeDisplay, durationDisplay, timeAgo);
	}

	private String resolvePlayerDisplay(Player player, Match match) {
		if (player == null) {
			return "-";
		}
		String name = safeStr(player.getName());
		if (match == null) {
			return "<white>" + name + "</white>";
		}
		if (isSpeedrunner(player, match)) {
			return UiColors.speedrunner(name);
		}
		if (match.getHunters().contains(player.getUniqueId())) {
			return UiColors.hunter(name);
		}
		return "<white>" + name + "</white>";
	}

	private String resolveRoleDisplay(Player player, Match match) {
		if (match == null || player == null) {
			return "-";
		}
		if (isSpeedrunner(player, match)) {
			return UiColors.roleSpeedrunner();
		}
		if (match.getHunters().contains(player.getUniqueId())) {
			return UiColors.roleHunter();
		}
		return "<white>Người chơi</white>";
	}

	private boolean isSpeedrunner(Player player, Match match) {
		if (match == null || match.getSpeedrunnerId() == null) {
			return false;
		}
		if (player == null) {
			return false;
		}
		return match.getSpeedrunnerId().equals(player.getUniqueId());
	}

	private String formatShortDuration(long totalSeconds) {
		long safeSeconds = Math.max(0L, totalSeconds);
		long minutes = safeSeconds / 60L;
		long seconds = safeSeconds % 60L;
		return String.format(Locale.US, "%02d:%02d", minutes, seconds);
	}

	private String formatPlaytime(long millis) {
		long safeMillis = Math.max(0L, millis);
		long totalSeconds = safeMillis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		return String.format(Locale.US, "%dh %02dm", hours, minutes);
	}

	private String formatDuration(long millis) {
		if (millis <= 0L) {
			return "Chưa có";
		}
		long totalSeconds = millis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0) {
			return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format(Locale.US, "%d:%02d", minutes, seconds);
	}

	private String formatWinrate(int wins, int total) {
		if (total <= 0) {
			return "0%";
		}
		double rate = (wins * 100.0) / total;
		return String.format(Locale.US, "%.1f%%", rate);
	}

	private String formatHistoryRole(String role) {
		if (role == null) {
			return "-";
		}
		if ("SPEEDRUNNER".equalsIgnoreCase(role)) {
			return UiColors.roleSpeedrunner();
		}
		if ("HUNTER".equalsIgnoreCase(role)) {
			return UiColors.roleHunter();
		}
		return "<white>Người chơi</white>";
	}

	private String formatHistoryOutcome(MatchHistoryEntry entry) {
		if (entry == null) {
			return "-";
		}
		return entry.isWon() ? "<green>Thắng</green>" : "<red>Thua</red>";
	}

	private String formatRelativeTime(long nowMillis, long timeMillis) {
		long delta = Math.max(0L, nowMillis - Math.max(0L, timeMillis));
		long minutes = delta / 60000L;
		if (minutes < 1) {
			return "vừa xong";
		}
		if (minutes < 60) {
			return minutes + "p trước";
		}
		long hours = minutes / 60L;
		if (hours < 24) {
			return hours + "h trước";
		}
		long days = hours / 24L;
		return days + "d trước";
	}

	private static final class ProfileSnapshot {
		private final long loadedAtMillis;
		private final String playtimeDisplay;
		private final String bestTimeDisplay;
		private final int kills;
		private final int hunterWins;
		private final int hunterTotal;
		private final String hunterWinrate;
		private final int speedrunnerWins;
		private final int speedrunnerTotal;
		private final String speedrunnerWinrate;
		private final java.util.List<HistoryLine> recentMatches;

		private ProfileSnapshot(long loadedAtMillis, String playtimeDisplay, String bestTimeDisplay, int kills,
				int hunterWins, int hunterTotal, String hunterWinrate,
				int speedrunnerWins, int speedrunnerTotal, String speedrunnerWinrate,
				java.util.List<HistoryLine> recentMatches) {
			this.loadedAtMillis = loadedAtMillis;
			this.playtimeDisplay = playtimeDisplay;
			this.bestTimeDisplay = bestTimeDisplay;
			this.kills = kills;
			this.hunterWins = hunterWins;
			this.hunterTotal = hunterTotal;
			this.hunterWinrate = hunterWinrate;
			this.speedrunnerWins = speedrunnerWins;
			this.speedrunnerTotal = speedrunnerTotal;
			this.speedrunnerWinrate = speedrunnerWinrate;
			this.recentMatches = recentMatches == null ? java.util.List.of() : recentMatches;
		}
	}

	private static final class HistoryLine {
		private final String matchId;
		private final String roleDisplay;
		private final String outcomeDisplay;
		private final String durationDisplay;
		private final String timeAgo;

		private HistoryLine(String matchId, String roleDisplay, String outcomeDisplay,
				String durationDisplay, String timeAgo) {
			this.matchId = matchId;
			this.roleDisplay = roleDisplay;
			this.outcomeDisplay = outcomeDisplay;
			this.durationDisplay = durationDisplay;
			this.timeAgo = timeAgo;
		}
	}

	private String formatWorldTime(dev.belikhun.lunamanhunt.model.match.WorldTimeSetting setting) {
		if (setting == null) {
			return "-";
		}
		return switch (setting) {
			case CYCLE -> "Tự động";
			case MORNING -> "Sáng";
			case NOON -> "Trưa";
			case NIGHT -> "Tối";
			case MIDNIGHT -> "Nửa đêm";
		};
	}

	private String formatWorldWeather(dev.belikhun.lunamanhunt.model.match.WorldWeatherSetting setting) {
		if (setting == null) {
			return "-";
		}
		return switch (setting) {
			case CYCLE -> "Tự động";
			case CLEAR -> "Nắng";
			case RAIN -> "Mưa";
			case THUNDERSTORM -> "Bão";
		};
	}

	private String formatWorldDifficulty(dev.belikhun.lunamanhunt.model.match.WorldDifficultySetting setting) {
		if (setting == null) {
			return "-";
		}
		return switch (setting) {
			case PEACEFUL -> "Hòa bình";
			case EASY -> "Dễ";
			case MEDIUM -> "Thường";
			case HARD -> "Khó";
		};
	}

	private String formatVisibility(dev.belikhun.lunamanhunt.model.match.MatchVisibility visibility) {
		if (visibility == null) {
			return "-";
		}
		return switch (visibility) {
			case PUBLIC -> "Công khai";
			case INVITE_ONLY -> "Chỉ mời";
			case PRIVATE -> "Riêng tư";
		};
	}

	private String resolveMatchStateDisplay(MatchState state) {
		if (state == null) {
			return "-";
		}
		return switch (state) {
			case LOBBY -> "<gray>Đang chờ</gray>";
			case WORLD_CREATING -> "<yellow>Đang tạo thế giới</yellow>";
			case WORLD_READY -> "<yellow>Đang chuẩn bị</yellow>";
			case RUN_PENDING -> "<aqua>Chờ bắt đầu</aqua>";
			case RUNNING -> "<green>Đang chạy</green>";
			case ENDING -> "<gold>Kết thúc</gold>";
			case CLEANUP -> "<gold>Thu dọn</gold>";
			case FINISHED -> "<gray>Đã xong</gray>";
		};
	}

	private String resolveWinnerDisplay(MatchWinner winner) {
		if (winner == null) {
			return "-";
		}
		return switch (winner) {
			case SPEEDRUNNER -> UiColors.roleSpeedrunner();
			case HUNTERS -> UiColors.roleHunter();
		};
	}

	private String resolvePendingHint(Player player, Match match) {
		if (player == null || match == null) {
			return "-";
		}
		if (isSpeedrunner(player, match)) {
			return "Đánh một " + UiColors.roleHunterLower() + " để bắt đầu";
		}
		return "Chờ " + UiColors.roleSpeedrunner() + " ra đòn";
	}

	private String resolveSeed(Match match) {
		if (match == null || match.getWorlds() == null) {
			return "-";
		}
		String overworldName = match.getWorlds().getOverworldName();
		if (overworldName == null || overworldName.isBlank()) {
			return "-";
		}
		World overworld = Bukkit.getWorld(overworldName);
		if (overworld == null) {
			return "-";
		}
		return String.valueOf(overworld.getSeed());
	}

	private String formatRunTimer(Match match) {
		if (match == null || match.getStartedAtMillis() == null) {
			return "00:00.000";
		}
		long elapsed = Math.max(0L, System.currentTimeMillis() - match.getStartedAtMillis());
		long totalSeconds = elapsed / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		long millis = elapsed % 1000L;
		return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis);
	}

	private String formatMatchDuration(Match match) {
		if (match == null) {
			return "-";
		}
		Long started = match.getStartedAtMillis();
		Long ended = match.getEndedAtMillis();
		if (started == null || ended == null) {
			return "-";
		}
		long elapsed = Math.max(0L, ended - started);
		long totalSeconds = elapsed / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		long millis = elapsed % 1000L;
		return String.format(Locale.US, "%02d:%02d.%03d", minutes, seconds, millis);
	}

	private String formatMilestone(Long millis) {
		if (millis == null || millis <= 0) {
			return "-";
		}
		long totalSeconds = millis / 1000L;
		long minutes = totalSeconds / 60L;
		long seconds = totalSeconds % 60L;
		return String.format(Locale.US, "%02d:%02d", minutes, seconds);
	}

	private String formatEta(long seconds) {
		if (seconds <= 0) {
			return "--:--s";
		}
		Duration duration = Duration.ofSeconds(seconds);
		long minutes = duration.toMinutes();
		long remaining = duration.minusMinutes(minutes).toSeconds();
		return String.format(Locale.US, "%02d:%02ds", minutes, remaining);
	}

	private int safeInt(String value, int def) {
		if (value == null || value.isBlank()) {
			return def;
		}
		try {
			return Integer.parseInt(value.trim());
		} catch (NumberFormatException ignored) {
			return def;
		}
	}

	private String formatProgressBar(int percent, int length) {
		int safeLength = Math.max(1, length);
		int clampedPercent = Math.max(0, Math.min(100, percent));
		int filled = (int) Math.round((clampedPercent / 100.0) * safeLength);
		String fillColor = clampedPercent >= 80 ? "green" : clampedPercent >= 45 ? "yellow" : "red";
		String fillPart = "█".repeat(Math.max(0, filled));
		String emptyPart = "░".repeat(Math.max(0, safeLength - filled));
		return "<" + fillColor + ">" + fillPart + "</" + fillColor + ">"
			+ "<dark_gray>" + emptyPart + "</dark_gray>";
	}

	private String formatDecimal(double value, int decimals) {
		if (!Double.isFinite(value)) {
			return "0";
		}
		int safeDecimals = Math.max(0, decimals);
		String format = "%." + safeDecimals + "f";
		return String.format(Locale.US, format, value);
	}

	private String resolvePlayerName(UUID playerId) {
		if (playerId == null) {
			return "-";
		}
		Player online = Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		var offline = Bukkit.getOfflinePlayer(playerId);
		String name = offline == null ? null : offline.getName();
		return name == null || name.isBlank() ? "Người chơi" : name;
	}

	private Sidebar ensureSidebar(Player player) {
		if (library == null) {
			return null;
		}
		PlayerEntry entry = players.computeIfAbsent(player.getUniqueId(), id -> new PlayerEntry());
		Sidebar sidebar = entry.sidebar;
		if (sidebar == null) {
			sidebar = library.createSidebar();
			entry.sidebar = sidebar;
			entry.lastEntityId = player.getEntityId();
			try {
				sidebar.addPlayer(player);
			} catch (Throwable ignored) {
			}
		}
		try {
			int entityId = player.getEntityId();
			if (entry.lastEntityId != entityId) {
				entry.lastEntityId = entityId;
				sidebar.addPlayer(player);
			}
		} catch (Throwable ignored) {
		}
		return sidebar;
	}

	private void applySidebarComponents(Player player, Sidebar sidebar, Component title,
			java.util.List<Component> lines) {
		if (sidebar == null) {
			return;
		}
		PlayerEntry entry = players.computeIfAbsent(player.getUniqueId(), id -> new PlayerEntry());
		sidebar.title(title);
		int last = entry.lastLineCount;
		int newSize = lines == null ? 0 : lines.size();
		int maxLines = Sidebar.MAX_LINES;
		try {
			maxLines = sidebar.maxLines();
		} catch (Throwable ignored) {
		}
		int limitNew = Math.min(newSize, maxLines);
		if (lines != null) {
			for (int idx = 0; idx < limitNew; idx++) {
				sidebar.line(idx, lines.get(idx));
			}
		}
		if (last > limitNew) {
			int limitOld = Math.min(last, maxLines);
			for (int idx = limitNew; idx < limitOld; idx++) {
				sidebar.line(idx, (Component) null);
			}
		}
		entry.lastLineCount = newSize;
	}

	private java.util.List<Component> parseLines(Player player, java.util.List<String> lines,
			Map<String, String> placeholders) {
		java.util.List<Component> out = new java.util.ArrayList<>(lines == null ? 0 : lines.size());
		if (lines == null || lines.isEmpty()) {
			return out;
		}
		for (String raw : lines) {
			String expanded = expandPlaceholders(player, raw, placeholders);
			if (expanded == null) {
				continue;
			}
			out.add(deserializeMini(expanded));
		}
		return out;
	}

	private Component parse(Player player, String raw, Map<String, String> placeholders) {
		String expanded = expandPlaceholders(player, raw, placeholders);
		if (expanded == null) {
			return Component.empty();
		}
		return deserializeMini(expanded);
	}

	private Component deserializeMini(String expanded) {
		if (expanded == null) {
			return Component.empty();
		}
		try {
			return MINI.deserialize(expanded);
		} catch (Throwable ignored) {
			return Text.c(expanded);
		}
	}

	private String expandPlaceholders(Player player, String raw, Map<String, String> placeholders) {
		if (raw == null) {
			return null;
		}
		String out = raw;
		if (placeholders != null) {
			for (var entry : placeholders.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				if (key == null) {
					continue;
				}
				out = out.replace("%" + key + "%", value == null ? "" : value);
			}
		}
		if (placeholderApiEnabled) {
			try {
				out = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, out);
			} catch (Throwable ignored) {
			}
		}
		return out;
	}

	private void cleanupOfflinePlayers() {
		if (players.isEmpty()) {
			return;
		}
		onlineIdsTmp.clear();
		for (Player p : Bukkit.getOnlinePlayers()) {
			onlineIdsTmp.add(p.getUniqueId());
		}
		players.entrySet().removeIf(entry -> {
			UUID id = entry.getKey();
			if (onlineIdsTmp.contains(id)) {
				return false;
			}
			PlayerEntry playerEntry = entry.getValue();
			try {
				if (playerEntry != null && playerEntry.sidebar != null) {
					playerEntry.sidebar.close();
				}
			} catch (Throwable ignored) {
			}
			profileCache.remove(id);
			return true;
		});
	}

	private void refreshPlaceholderApiEnabledIfNeeded(long nowMs) {
		if (nowMs < placeholderApiNextRefreshMs) {
			return;
		}
		placeholderApiNextRefreshMs = nowMs + PLACEHOLDER_REFRESH_MS;
		boolean enabled = false;
		try {
			enabled = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
		} catch (Throwable ignored) {
			enabled = false;
		}
		placeholderApiEnabled = enabled;
	}

	private String safeStr(String value) {
		if (value == null) {
			return "-";
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? "-" : trimmed;
	}

	private String cfgString(String path, String def) {
		String value = plugin.getConfig().getString(path);
		return value != null ? value : def;
	}

	private java.util.List<String> cfgStringList(String path, java.util.List<String> def) {
		java.util.List<String> out = plugin.getConfig().getStringList(path);
		if (out != null && !out.isEmpty()) {
			return out;
		}
		return def;
	}

	private int cfgInt(String path, int def) {
		return plugin.getConfig().getInt(path, def);
	}

	private boolean cfgBool(String path, boolean def) {
		if (plugin.getConfig().contains(path)) {
			return plugin.getConfig().getBoolean(path, def);
		}
		return def;
	}
}
