package dev.belikhun.lunamanhunt.service.integration;

import dev.belikhun.lunamanhunt.LunaManhuntPlugin;
import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.UiColors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class LunaPlaceholderExpansion extends PlaceholderExpansion {
	private final LunaManhuntPlugin plugin;
	private final LobbyManager lobbyManager;

	public LunaPlaceholderExpansion(LunaManhuntPlugin plugin, LobbyManager lobbyManager) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
	}

	@Override
	public String getIdentifier() {
		return "lunamanhunt";
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
		if (player == null || params == null || params.isBlank()) {
			return "";
		}

		String key = params.toLowerCase(Locale.ROOT);
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		String role = resolveRole(player.getUniqueId(), match);

		return switch (key) {
			case "match_id" -> match == null || match.getId() == null || match.getId().isBlank()
				? "-"
				: match.getId();
			case "role" -> role;
			case "role_display" -> resolveRoleDisplay(role);
			case "role_color" -> switch (role) {
				case "speedrunner" -> UiColors.ROLE_SPEEDRUNNER + ":" + UiColors.ROLE_SPEEDRUNNER_ACCENT;
				case "hunter" -> UiColors.ROLE_HUNTER + ":" + UiColors.ROLE_HUNTER_ACCENT;
				default -> "#FFFFFF";
			};
			case "match_state" -> resolveMatchState(match);
			default -> null;
		};
	}

	private String resolveRoleDisplay(String role) {
		if (role == null || role.isBlank()) {
			return "Không có";
		}
		return switch (role) {
			case "speedrunner" -> "Speedrunner";
			case "hunter" -> "Thợ săn";
			case "lobby" -> "Sảnh";
			default -> "Không có";
		};
	}

	private String resolveMatchState(Match match) {
		if (match == null || match.getState() == null) {
			return "none";
		}
		return match.getState().name().toLowerCase(Locale.ROOT);
	}

	private String resolveRole(UUID playerId, Match match) {
		if (match == null || playerId == null) {
			return "none";
		}
		if (playerId.equals(match.getSpeedrunnerId())) {
			return "speedrunner";
		}
		if (match.getHunters().contains(playerId)) {
			return "hunter";
		}
		if (match.getLobbyPlayers().contains(playerId)) {
			return "lobby";
		}
		return "none";
	}
}
