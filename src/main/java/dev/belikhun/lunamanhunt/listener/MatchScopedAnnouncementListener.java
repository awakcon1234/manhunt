package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.lang.reflect.Method;

public class MatchScopedAnnouncementListener implements Listener {
	private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;

	public MatchScopedAnnouncementListener(JavaPlugin plugin, LobbyManager lobbyManager) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null || event.getEntity() == null) {
			return;
		}
		Player player = event.getEntity();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (!isScopedMessageActive(match)) {
			return;
		}

		Component deathMessage = event.deathMessage();

		event.deathMessage(null);

		if (deathMessage == null) {
			return;
		}

		String format = plugin.getConfig().getString("match.chat.deathMessageFormat",
			"<gray>[#<white>%match_id%</white>]</gray> <red>☠</red> %player% <gray>%death_message%</gray>");
		for (UUID playerId : collectMembers(match)) {
			Player target = Bukkit.getPlayer(playerId);
			if (target == null) {
				continue;
			}
			String playerName = Text.playerNameDisplay(player.getUniqueId(), target);
			String playerColored = resolveRoleColoredPlayer(match, player.getUniqueId());
			String roleIcon = resolveRoleIcon(match, player.getUniqueId());
			String localizedDeathMessage = localizeComponentPlainText(deathMessage, target);
			String deathMessageBody = stripVictimName(localizedDeathMessage,
				Text.plainPlayerName(player.getUniqueId()),
				player.getName());
			deathMessageBody = escapeMiniMessage(deathMessageBody);
			if (deathMessageBody.isBlank()) {
				deathMessageBody = escapeMiniMessage(localizedDeathMessage);
			}
			if (localizedDeathMessage.isBlank()) {
				continue;
			}
			String rendered = resolveTemplate(format, safe(match.getId()), playerName, playerColored,
				roleIcon, deathMessageBody, deathMessageBody, null);
			rendered = applyPlaceholderApi(target, rendered);
			Text.tell(target, rendered);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onAdvancement(PlayerAdvancementDoneEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		suppressVanillaAdvancementBroadcast(event);
		String key = event.getAdvancement().getKey().getKey();
		if (key != null && key.startsWith("recipes/")) {
			return;
		}
		Player player = event.getPlayer();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}

		Component displayName = event.getAdvancement().displayName();
		if (displayName == null) {
			return;
		}

		String format = plugin.getConfig().getString("match.chat.advancementMessageFormat",
			"<gray>[#<white>%match_id%</white>]</gray> <yellow>★</yellow> %player% <gray>đạt thành tựu</gray> %advancement%");
		for (UUID playerId : collectMembers(match)) {
			Player target = Bukkit.getPlayer(playerId);
			if (target == null) {
				continue;
			}
			String playerName = Text.playerNameDisplay(player.getUniqueId(), target);
			String playerColored = resolveRoleColoredPlayer(match, player.getUniqueId());
			String roleIcon = resolveRoleIcon(match, player.getUniqueId());
			String localizedTitle = localizeComponentMiniMessage(displayName, target);
			if (localizedTitle.isBlank()) {
				continue;
			}
			String rendered = resolveTemplate(format, safe(match.getId()), playerName, playerColored,
				roleIcon, null, null, localizedTitle);
			rendered = applyPlaceholderApi(target, rendered);
			Text.tell(target, rendered);
		}
	}

	private void suppressVanillaAdvancementBroadcast(PlayerAdvancementDoneEvent event) {
		try {
			var componentSetter = event.getClass().getMethod("message", Component.class);
			componentSetter.invoke(event, (Object) null);
			return;
		} catch (Throwable ignored) {
		}
		try {
			var componentSetter = event.getClass().getMethod("setMessage", Component.class);
			componentSetter.invoke(event, (Object) null);
			return;
		} catch (Throwable ignored) {
		}
		try {
			var stringSetter = event.getClass().getMethod("setMessage", String.class);
			stringSetter.invoke(event, (Object) null);
		} catch (Throwable ignored) {
		}
	}

	private String localizeComponentMiniMessage(Component source, Player target) {
		if (source == null || target == null) {
			return "";
		}
		Locale locale = resolveLocale(target);
		Component rendered = GlobalTranslator.render(source, locale);
		String serialized = MINI_MESSAGE.serialize(rendered);
		return serialized == null ? "" : serialized;
	}

	private String localizeComponentPlainText(Component source, Player target) {
		if (source == null || target == null) {
			return "";
		}
		Locale locale = resolveLocale(target);
		Component rendered = GlobalTranslator.render(source, locale);
		String serialized = PlainTextComponentSerializer.plainText().serialize(rendered);
		return serialized == null ? "" : serialized;
	}

	private String stripVictimName(String message, String... names) {
		if (message == null || message.isBlank() || names == null || names.length == 0) {
			return message == null ? "" : message;
		}

		String trimmed = message.trim();
		for (String rawName : names) {
			if (rawName == null || rawName.isBlank()) {
				continue;
			}
			String name = rawName.trim();
			if (trimmed.equals(name)) {
				return "";
			}
			if (trimmed.regionMatches(true, 0, name, 0, name.length())) {
				int nextIndex = name.length();
				if (trimmed.length() > nextIndex && Character.isWhitespace(trimmed.charAt(nextIndex))) {
					return trimmed.substring(nextIndex).trim();
				}
			}
		}

		return trimmed;
	}

	private Locale resolveLocale(Player player) {
		if (player == null) {
			return Locale.getDefault();
		}

		return player.locale();
	}

	private boolean isScopedMessageActive(Match match) {
		if (match == null || match.getState() == null) {
			return false;
		}
		return switch (match.getState()) {
			case RUN_PENDING, RUNNING, ENDING, CLEANUP -> true;
			default -> false;
		};
	}

	private void broadcastToMatch(Match match, String message) {
		for (UUID playerId : collectMembers(match)) {
			Player target = Bukkit.getPlayer(playerId);
			if (target != null) {
				Text.tell(target, message);
			}
		}
	}

	private Set<UUID> collectMembers(Match match) {
		Set<UUID> members = new HashSet<>(match.getLobbyPlayers());
		members.addAll(match.getHunters());
		if (match.getSpeedrunnerId() != null) {
			members.add(match.getSpeedrunnerId());
		}
		return members;
	}

	private String escapeMiniMessage(String text) {
		if (text == null || text.isEmpty()) {
			return "";
		}
		return text.replace("<", "\\<").replace(">", "\\>");
	}

	private String safe(String value) {
		return value == null ? "-" : value;
	}

	private String resolveTemplate(String format, String matchId, String playerDisplay, String playerColored,
			String roleIcon, String message, String deathMessage, String advancement) {
		String value = format == null ? "" : format;
		value = replaceToken(value, "match_id", safe(matchId));
		value = replaceToken(value, "player", playerDisplay == null ? "" : playerDisplay);
		value = replaceToken(value, "player_colored", playerColored == null ? "" : playerColored);
		value = replaceToken(value, "role_icon", roleIcon == null ? "" : roleIcon);
		value = replaceToken(value, "message", message == null ? "" : message);
		value = replaceToken(value, "death_message", deathMessage == null ? "" : deathMessage);
		value = replaceToken(value, "advancement", advancement == null ? "" : advancement);
		return value;
	}

	private String resolveRoleIcon(Match match, UUID playerId) {
		if (match == null || playerId == null) {
			return "<gray>⌛</gray>";
		}
		if (match.getSpeedrunnerId() != null && match.getSpeedrunnerId().equals(playerId)) {
			return "<gold>★</gold>";
		}
		if (match.getHunters().contains(playerId)) {
			return "<red>⚔</red>";
		}
		return "<gray>⌛</gray>";
	}

	private String resolveRoleColoredPlayer(Match match, UUID playerId) {
		String value = escapeMiniMessage(Text.plainPlayerName(playerId));
		if (match == null || playerId == null) {
			return value;
		}
		if (match.getSpeedrunnerId() != null && match.getSpeedrunnerId().equals(playerId)) {
			return UiColors.speedrunner(value);
		}
		if (match.getHunters().contains(playerId)) {
			return UiColors.hunter(value);
		}
		return value;
	}

	private String replaceToken(String text, String key, String value) {
		if (text == null) {
			return "";
		}
		String safeValue = value == null ? "" : value;
		return text
			.replace("%" + key + "%", safeValue)
			.replace("{" + key + "}", safeValue);
	}

	private String applyPlaceholderApi(Player player, String text) {
		if (player == null || text == null || text.isBlank()) {
			return text == null ? "" : text;
		}
		if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			return text;
		}
		try {
			Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
			Method method = clazz.getMethod("setPlaceholders", Player.class, String.class);
			Object result = method.invoke(null, player, text);
			return result instanceof String str ? str : text;
		} catch (Throwable ignored) {
			return text;
		}
	}
}
