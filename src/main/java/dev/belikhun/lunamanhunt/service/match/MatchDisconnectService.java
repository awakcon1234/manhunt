package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MatchDisconnectService {
	private static final int SPEEDRUNNER_RECONNECT_TICKS = 20 * 60;

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final WinConditionService winConditionService;
	private final ConcurrentMap<String, BukkitTask> speedrunnerReconnectTasks = new ConcurrentHashMap<>();

	public MatchDisconnectService(JavaPlugin plugin, LobbyManager lobbyManager, WinConditionService winConditionService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.winConditionService = Objects.requireNonNull(winConditionService, "winConditionService");
	}

	public void handlePlayerQuit(Player player) {
		if (player == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (!isMatchOngoing(match)) {
			return;
		}
		if (isSpeedrunner(match, player.getUniqueId())) {
			startSpeedrunnerReconnectTimer(match);
			broadcast(match, "<yellow>⌛</yellow> " + UiColors.speedrunner("Speedrunner")
				+ " đã mất kết nối. Có <white>60</white> giây để quay lại.");
			return;
		}
		if (match.getHunters().contains(player.getUniqueId()) && areAllHuntersOffline(match)) {
			clearSpeedrunnerReconnect(match.getId());
			winConditionService.handleAllHuntersDisconnected(match);
		}
	}

	public void handlePlayerJoin(Player player) {
		if (player == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (!isMatchOngoing(match)) {
			return;
		}
		if (isSpeedrunner(match, player.getUniqueId()) && clearSpeedrunnerReconnect(match.getId())) {
			broadcast(match, "<green>✔</green> " + UiColors.speedrunner("Speedrunner")
				+ " đã quay lại. Trận đấu tiếp tục!");
		}
	}

	private void startSpeedrunnerReconnectTimer(Match match) {
		if (match == null || speedrunnerReconnectTasks.containsKey(match.getId())) {
			return;
		}
		BukkitTask task = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
			speedrunnerReconnectTasks.remove(match.getId());
			if (!isMatchOngoing(match)) {
				return;
			}
			UUID speedrunnerId = match.getSpeedrunnerId();
			if (speedrunnerId == null || Bukkit.getPlayer(speedrunnerId) != null) {
				return;
			}
			winConditionService.handleSpeedrunnerDisconnect(match);
		}, SPEEDRUNNER_RECONNECT_TICKS);
		speedrunnerReconnectTasks.put(match.getId(), task);
	}

	private boolean clearSpeedrunnerReconnect(String matchId) {
		BukkitTask task = speedrunnerReconnectTasks.remove(matchId);
		if (task != null) {
			task.cancel();
			return true;
		}
		return false;
	}

	private boolean areAllHuntersOffline(Match match) {
		if (match == null) {
			return false;
		}
		for (UUID hunterId : Set.copyOf(match.getHunters())) {
			if (Bukkit.getPlayer(hunterId) != null) {
				return false;
			}
		}
		return true;
	}

	private boolean isMatchOngoing(Match match) {
		if (match == null) {
			return false;
		}
		MatchState state = match.getState();
		return state != MatchState.LOBBY && state != MatchState.FINISHED
			&& state != MatchState.ENDING && state != MatchState.CLEANUP;
	}

	private boolean isSpeedrunner(Match match, UUID playerId) {
		return match != null && playerId != null && playerId.equals(match.getSpeedrunnerId());
	}

	private void broadcast(Match match, String message) {
		if (match == null || message == null) {
			return;
		}
		Set<UUID> members = new java.util.HashSet<>(match.getLobbyPlayers());
		members.addAll(match.getHunters());
		if (match.getSpeedrunnerId() != null) {
			members.add(match.getSpeedrunnerId());
		}
		for (UUID memberId : Set.copyOf(members)) {
			Player member = Bukkit.getPlayer(memberId);
			if (member != null) {
				Text.msg(member, message);
			}
		}
	}
}
