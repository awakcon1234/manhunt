package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;

public class MatchChatListener implements Listener {
	private static final long DUPLICATE_WINDOW_MS = 500L;

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final GuiService guiService;
	private final Map<UUID, ChatFingerprint> recentMessages = new ConcurrentHashMap<>();

	public MatchChatListener(JavaPlugin plugin, LobbyManager lobbyManager, GuiService guiService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.guiService = Objects.requireNonNull(guiService, "guiService");
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onChat(AsyncChatEvent event) {
		if (event == null) {
			return;
		}
		Player sender = event.getPlayer();
		if (sender == null) {
			return;
		}
		String plainMessage = PlainTextComponentSerializer.plainText().serialize(event.message());
		if (!routeMatchChat(sender, plainMessage)) {
			return;
		}
		event.viewers().clear();
		event.message(Component.empty());
		event.setCancelled(true);
	}

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOWEST)
	public void onLegacyChat(AsyncPlayerChatEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		if (!routeMatchChat(event.getPlayer(), event.getMessage())) {
			return;
		}
		event.getRecipients().clear();
		event.setCancelled(true);
	}

	private boolean routeMatchChat(Player sender, String plainMessage) {
		if (guiService.hasSeedInput(sender) || guiService.hasGenVersionInput(sender)) {
			return false;
		}

		Match match = lobbyManager.getMatchForPlayer(sender.getUniqueId());
		if (!isInternalChatActive(match)) {
			return false;
		}
		if (plainMessage == null || plainMessage.isBlank()) {
			return true;
		}
		if (isDuplicate(sender.getUniqueId(), plainMessage)) {
			return true;
		}

		UUID senderId = sender.getUniqueId();
		String senderName = sender.getName();
		String matchId = safe(match.getId());
		String format = plugin.getConfig().getString("match.chat.chatFormat",
			"<gray>[#<white>%match_id%</white>]</gray> %role_icon% %player_colored%<gray>:</gray> <white>%message%</white>");

		Bukkit.getScheduler().runTask(plugin, () -> broadcastToMatch(match, target -> {
			String playerDisplay = Text.playerNameDisplay(senderId, target);
			String rendered = resolveTemplate(format, match, senderId, matchId, playerDisplay,
				escapeMiniMessage(plainMessage), null, null);
			return applyPlaceholderApi(target, rendered);
		}));
		plugin.getLogger().info("[MatchChat#" + matchId + "] " + senderName + ": " + plainMessage);
		return true;
	}

	private boolean isInternalChatActive(Match match) {
		if (match == null || match.getState() == null) {
			return false;
		}
		return switch (match.getState()) {
			case RUN_PENDING, RUNNING, ENDING, CLEANUP -> true;
			default -> false;
		};
	}

	private void broadcastToMatch(Match match, java.util.function.Function<Player, String> resolver) {
		if (match == null || resolver == null) {
			return;
		}
		for (UUID playerId : collectMembers(match)) {
			Player target = Bukkit.getPlayer(playerId);
			if (target != null) {
				String message = resolver.apply(target);
				if (message != null && !message.isBlank()) {
					Text.tell(target, message);
				}
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

	private String resolveTemplate(String format, Match match, UUID senderId, String matchId,
			String playerDisplay, String message,
			String deathMessage, String advancement) {
		String value = format == null || format.isBlank()
			? "<gray>[#<white>%match_id%</white>]</gray> %role_icon% %player_colored%<gray>:</gray> <white>%message%</white>"
			: format;
		String roleIcon = resolveRoleIcon(match, senderId);
		String playerColored = resolveRoleColoredPlayer(match, senderId);
		value = replaceToken(value, "match_id", safe(matchId));
		value = replaceToken(value, "player", playerDisplay == null ? "" : playerDisplay);
		value = replaceToken(value, "player_colored", playerColored);
		value = replaceToken(value, "role_icon", roleIcon);
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

	private boolean isDuplicate(UUID playerId, String message) {
		long now = System.currentTimeMillis();
		ChatFingerprint last = recentMessages.get(playerId);
		recentMessages.put(playerId, new ChatFingerprint(message, now));
		if (last == null) {
			return false;
		}
		if (!last.message().equals(message)) {
			return false;
		}
		return now - last.atMillis() <= DUPLICATE_WINDOW_MS;
	}

	private record ChatFingerprint(String message, long atMillis) {
	}
}
