package dev.belikhun.lunamanhunt.service.scoreboard;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.card.ActiveCard;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.point.PointService;
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
	private final Map<UUID, PlayerEntry> players = new HashMap<>();
	private final Set<UUID> onlineIdsTmp = new HashSet<>();
	private ScoreboardLibrary library;
	private int taskId = -1;
	private boolean placeholderApiEnabled = false;
	private long placeholderApiNextRefreshMs = 0L;
	private boolean enabled = true;
	private int updatePeriodTicks = DEFAULT_UPDATE_TICKS;

	private static final class PlayerEntry {
		private Sidebar sidebar;
		private int lastLineCount;
		private int lastEntityId;
	}

	public ScoreboardService(JavaPlugin plugin, LobbyManager lobbyManager, CardService cardService,
			MatchLifecycleService lifecycleService, PointService pointService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.lifecycleService = Objects.requireNonNull(lifecycleService, "lifecycleService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
	}

	public void start() {
		if (taskId != -1) {
			return;
		}
		this.enabled = cfgBool("scoreboard.enabled", true);
		if (!enabled) {
			return;
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

		for (Player player : Bukkit.getOnlinePlayers()) {
			try {
				tickPlayer(player);
			} catch (Throwable ignored) {
			}
		}
	}

	private void tickPlayer(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		String template = resolveTemplate(player, match);
		Map<String, String> placeholders = buildPlaceholders(player, match);
		Component title = parse(player, cfgString("scoreboard.templates." + template + ".title", ""), placeholders);
		java.util.List<Component> lines;
		if (TEMPLATE_PLAYING_SPEEDRUNNER.equals(template) || TEMPLATE_PLAYING_HUNTER.equals(template)) {
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

		return placeholders;
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
			return UiColors.speedrunner("Speedrunner");
		}
		if (match.getHunters().contains(player.getUniqueId())) {
			return UiColors.hunter("Thợ săn");
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
			case SPEEDRUNNER -> UiColors.speedrunner("Speedrunner");
			case HUNTERS -> UiColors.hunter("Thợ săn");
		};
	}

	private String resolvePendingHint(Player player, Match match) {
		if (player == null || match == null) {
			return "-";
		}
		if (isSpeedrunner(player, match)) {
			return "Đánh một " + UiColors.hunter("thợ săn") + " để bắt đầu";
		}
		return "Chờ " + UiColors.speedrunner("Speedrunner") + " ra đòn";
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

	private String formatDecimal(double value, int decimals) {
		if (!Double.isFinite(value)) {
			return "0";
		}
		String format = "%0." + Math.max(0, decimals) + "f";
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
