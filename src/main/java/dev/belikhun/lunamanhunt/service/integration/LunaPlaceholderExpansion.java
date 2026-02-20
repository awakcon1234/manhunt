package dev.belikhun.lunamanhunt.service.integration;

import dev.belikhun.lunamanhunt.LunaManhuntPlugin;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.profile.MatchHistoryEntry;
import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.util.MatchDisplayFormatter;
import dev.belikhun.lunamanhunt.util.TimeFormat;
import dev.belikhun.lunamanhunt.util.UiColors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LunaPlaceholderExpansion extends PlaceholderExpansion {
	private static final long PROFILE_CACHE_MS = 5000L;

	private final LunaManhuntPlugin plugin;
	private final LobbyManager lobbyManager;
	private final ProfileStore profileStore;
	private final Map<UUID, CachedProfileSnapshot> profileCache = new ConcurrentHashMap<>();

	private static final class CachedProfileSnapshot {
		private final long loadedAtMillis;
		private final ProfileSnapshot snapshot;

		private CachedProfileSnapshot(long loadedAtMillis, ProfileSnapshot snapshot) {
			this.loadedAtMillis = loadedAtMillis;
			this.snapshot = snapshot;
		}
	}

	private static final class ProfileSnapshot {
		private final int wins;
		private final int losses;
		private final int totalMatches;
		private final String winRate;
		private final String bestTime;
		private final int huntersKilled;

		private ProfileSnapshot(int wins, int losses, int totalMatches, String winRate, String bestTime,
				int huntersKilled) {
			this.wins = wins;
			this.losses = losses;
			this.totalMatches = totalMatches;
			this.winRate = winRate;
			this.bestTime = bestTime;
			this.huntersKilled = huntersKilled;
		}
	}

	public LunaPlaceholderExpansion(LunaManhuntPlugin plugin, LobbyManager lobbyManager, ProfileStore profileStore) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
	}

	@Override
	public String getIdentifier() {
		return "mh";
	}

	@Override
	public String getAuthor() {
		return "Belikhun";
	}

	@Override
	public String getVersion() {
		return plugin.getPluginMeta().getVersion();
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, String params) {
		return resolvePlaceholder(player == null ? null : player.getUniqueId(), params, player);
	}

	@Override
	public String onRequest(OfflinePlayer player, String params) {
		return resolvePlaceholder(player == null ? null : player.getUniqueId(), params, player);
	}

	private String resolvePlaceholder(UUID playerId, String params, OfflinePlayer playerContext) {
		if (params == null || params.isBlank()) {
			return "";
		}

		try {
			String key = params.toLowerCase(Locale.ROOT);
			Match match = playerId == null ? null : lobbyManager.getMatchForPlayer(playerId);
			String role = resolveRole(playerId, match);
			ProfileSnapshot profile = playerId == null ? emptyProfileSnapshot() : getProfileSnapshot(playerId);

			String value = switch (key) {
				case "match_exists" -> match == null ? "false" : "true";
				case "match_id" -> match == null || match.getId() == null || match.getId().isBlank()
					? "-"
					: match.getId();
				case "match_id_colored" -> match == null ? UiColors.matchIdText("-") : UiColors.matchIdText(match.getId());
				case "match_id_chip" -> match == null ? UiColors.matchIdChip("-") : UiColors.matchIdChip(match.getId());
				case "match_id_hash" -> match == null || match.getId() == null || match.getId().isBlank()
					? "#-"
					: "#" + match.getId();
				case "match_players" -> String.valueOf(safeLobbyPlayersSize(match));
				case "match_hunters" -> String.valueOf(safeHuntersSize(match));
				case "match_owner" -> resolvePlayerName(match == null ? null : match.getOwnerId());
				case "match_speedrunner" -> resolvePlayerName(match == null ? null : match.getSpeedrunnerId());
				case "match_timer" -> resolveMatchTimer(match);
				case "role" -> role;
				case "role_display" -> resolveRoleDisplay(role);
				case "role_color" -> switch (role) {
					case "speedrunner" -> UiColors.ROLE_SPEEDRUNNER + ":" + UiColors.ROLE_SPEEDRUNNER_ACCENT;
					case "hunter" -> UiColors.ROLE_HUNTER + ":" + UiColors.ROLE_HUNTER_ACCENT;
					default -> "#FFFFFF";
				};
				case "match_state" -> resolveMatchState(match);
				case "match_state_display" -> resolveMatchStateDisplay(match);
				case "profile_total_wins" -> String.valueOf(profile.wins);
				case "profile_total_losses" -> String.valueOf(profile.losses);
				case "profile_total_matches" -> String.valueOf(profile.totalMatches);
				case "profile_win_rate" -> profile.winRate;
				case "profile_best_time" -> profile.bestTime;
				case "profile_kills" -> String.valueOf(profile.huntersKilled);
				case "profile_hunter_kills" -> String.valueOf(profile.huntersKilled);
				default -> null;
			};

			if (value == null) {
				debugLog("unknown:" + key,
					"[PAPI] Placeholder chưa hỗ trợ: %mh_" + key + "%"
						+ " | player=" + debugPlayerName(playerContext, playerId));
				return null;
			}

			if (playerId == null) {
				debugLog("contextless:" + key,
					"[PAPI] Placeholder gọi không có player context: %mh_" + key + "%");
			}

			return value;
		} catch (Throwable throwable) {
			debugLog("error:" + params,
				"[PAPI] Lỗi xử lý placeholder %mh_" + params + "%: " + throwable.getClass().getSimpleName()
					+ " - " + (throwable.getMessage() == null ? "(không rõ)" : throwable.getMessage()));
			return "-";
		}
	}

	private ProfileSnapshot emptyProfileSnapshot() {
		return new ProfileSnapshot(0, 0, 0, "0.0", "--:--.---", 0);
	}

	private void debugLog(String bucket, String message) {
		if (!plugin.getConfig().getBoolean("debug.placeholderapi", false)) {
			return;
		}
		plugin.getLogger().info(message);
	}

	private String debugPlayerName(OfflinePlayer player, UUID playerId) {
		if (player != null && player.getName() != null && !player.getName().isBlank()) {
			return player.getName();
		}
		if (playerId == null) {
			return "null";
		}
		String id = playerId.toString();
		return id.length() <= 8 ? id : id.substring(0, 8);
	}

	private String resolveRoleDisplay(String role) {
		return MatchDisplayFormatter.formatRoleDisplay(role);
	}

	private String resolveMatchState(Match match) {
		if (match == null || match.getState() == null) {
			return "none";
		}
		return match.getState().name().toLowerCase(Locale.ROOT);
	}

	private String resolveMatchStateDisplay(Match match) {
		return MatchDisplayFormatter.formatMatchStateDisplay(match == null ? null : match.getState());
	}

	private String resolveMatchTimer(Match match) {
		if (match == null) {
			return "--:--.---";
		}
		Long startedAt = match.getStartedAtMillis();
		if (startedAt == null || startedAt <= 0) {
			return "00:00.000";
		}
		long endAt = match.getEndedAtMillis() != null && match.getEndedAtMillis() > 0
			? match.getEndedAtMillis()
			: System.currentTimeMillis();
		long elapsed = Math.max(0L, endAt - startedAt);
		return formatTimer(elapsed);
	}

	private String formatTimer(long millis) {
		return TimeFormat.formatRunTimer(millis);
	}

	private String resolvePlayerName(UUID playerId) {
		if (playerId == null) {
			return "-";
		}
		Player online = Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		String id = playerId.toString();
		return id.length() <= 8 ? id : id.substring(0, 8);
	}

	private ProfileSnapshot getProfileSnapshot(UUID playerId) {
		long now = System.currentTimeMillis();
		CachedProfileSnapshot cached = profileCache.get(playerId);
		if (cached != null && now - cached.loadedAtMillis <= PROFILE_CACHE_MS) {
			return cached.snapshot;
		}
		ProfileSnapshot snapshot = loadProfileSnapshot(playerId);
		profileCache.put(playerId, new CachedProfileSnapshot(now, snapshot));
		return snapshot;
	}

	private ProfileSnapshot loadProfileSnapshot(UUID playerId) {
		PlayerProfile profile;
		try {
			profile = profileStore.load(playerId);
		} catch (Exception ignored) {
			profile = new PlayerProfile(playerId);
		}

		int wins = Math.max(0, profile.getTotalWins());
		int losses = Math.max(0, profile.getTotalLosses());
		int total = Math.max(profile.getMatchHistory().size(), wins + losses);
		double rate = total <= 0 ? 0.0 : (wins * 100.0) / total;

		int huntersKilled = 0;
		for (MatchHistoryEntry entry : profile.getMatchHistory()) {
			if (entry == null) {
				continue;
			}
			huntersKilled += Math.max(0, entry.getHuntersKilled());
		}

		String bestTime = profile.getBestTimeMillis() > 0
			? formatTimer(profile.getBestTimeMillis())
			: "--:--.---";

		return new ProfileSnapshot(
			wins,
			losses,
			total,
			String.format(Locale.US, "%.1f", rate),
			bestTime,
			huntersKilled
		);
	}

	private String resolveRole(UUID playerId, Match match) {
		if (match == null || playerId == null) {
			return "none";
		}
		if (playerId.equals(match.getSpeedrunnerId())) {
			return "speedrunner";
		}
		if (safeHuntersContains(match, playerId)) {
			return "hunter";
		}
		if (safeLobbyPlayersContains(match, playerId)) {
			return "lobby";
		}
		return "none";
	}

	private int safeLobbyPlayersSize(Match match) {
		if (match == null) {
			return 0;
		}
		try {
			return match.getLobbyPlayers().size();
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private int safeHuntersSize(Match match) {
		if (match == null) {
			return 0;
		}
		try {
			return match.getHunters().size();
		} catch (Throwable ignored) {
			return 0;
		}
	}

	private boolean safeLobbyPlayersContains(Match match, UUID playerId) {
		if (match == null || playerId == null) {
			return false;
		}
		try {
			return match.getLobbyPlayers().contains(playerId);
		} catch (Throwable ignored) {
			return false;
		}
	}

	private boolean safeHuntersContains(Match match, UUID playerId) {
		if (match == null || playerId == null) {
			return false;
		}
		try {
			return match.getHunters().contains(playerId);
		} catch (Throwable ignored) {
			return false;
		}
	}
}
