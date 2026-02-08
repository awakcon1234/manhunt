package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class WinConditionService {
	private final MatchManager matchManager;
	private final LobbyManager lobbyManager;
	private final ProfileStore profileStore;

	public WinConditionService(MatchManager matchManager, LobbyManager lobbyManager, ProfileStore profileStore) {
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.profileStore = profileStore;
	}

	public boolean handleHunterKill(Player killer, Player victim) {
		if (killer == null || victim == null) {
			return false;
		}

		Match match = lobbyManager.getMatchForPlayer(victim.getUniqueId());
		if (!isMatchRunning(match)) {
			return false;
		}
		if (!sameMatch(match, killer.getUniqueId())) {
			return false;
		}
		if (!isSpeedrunner(match, victim.getUniqueId())) {
			return false;
		}
		if (!match.getHunters().contains(killer.getUniqueId())) {
			return false;
		}

		endWithHunters(match);
		return true;
	}

	public boolean handleDragonKill(Player killer) {
		if (killer == null) {
			return false;
		}

		Match match = lobbyManager.getMatchForPlayer(killer.getUniqueId());
		if (!isMatchRunning(match)) {
			return false;
		}
		if (!isSpeedrunner(match, killer.getUniqueId())) {
			return false;
		}

		endWithSpeedrunner(match);
		return true;
	}

	public boolean handleSpeedrunnerForfeit(Player speedrunner) {
		if (speedrunner == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(speedrunner.getUniqueId());
		if (!isMatchActive(match)) {
			return false;
		}
		if (!isSpeedrunner(match, speedrunner.getUniqueId())) {
			return false;
		}
		endWithHunters(match, "<red>❌</red> Speedrunner đã rời trận. Thợ săn chiến thắng!");
		return true;
	}

	private boolean isMatchRunning(Match match) {
		return match != null && match.getState() == MatchState.RUNNING;
	}

	private boolean isMatchActive(Match match) {
		return match != null && match.getState() != MatchState.LOBBY && match.getState() != MatchState.FINISHED;
	}

	private boolean sameMatch(Match match, UUID playerId) {
		Match playerMatch = lobbyManager.getMatchForPlayer(playerId);
		return playerMatch != null && playerMatch.getId().equals(match.getId());
	}

	private boolean isSpeedrunner(Match match, UUID playerId) {
		UUID speedrunnerId = match.getSpeedrunnerId();
		return speedrunnerId != null && speedrunnerId.equals(playerId);
	}

	private void endWithHunters(Match match) {
		endWithHunters(match, "<green>✔</green> Thợ săn đã chiến thắng!");
	}

	private void endWithHunters(Match match, String message) {
		match.setWinner(MatchWinner.HUNTERS);
		matchManager.endMatch(match);
		broadcastMatch(match, message);
	}

	private void endWithSpeedrunner(Match match) {
		match.setWinner(MatchWinner.SPEEDRUNNER);
		matchManager.endMatch(match);
		broadcastMatch(match, "<green>✔</green> Speedrunner đã chiến thắng!");
		recordSpeedrunTime(match);
	}

	private void recordSpeedrunTime(Match match) {
		if (profileStore == null) {
			return;
		}
		Long startedAt = match.getStartedAtMillis();
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (startedAt == null || speedrunnerId == null) {
			return;
		}
		long elapsed = Math.max(0L, System.currentTimeMillis() - startedAt);
		try {
			var profile = profileStore.load(speedrunnerId);
			profile.getRunTimesMillis().add(elapsed);
			if (profile.getBestTimeMillis() <= 0 || elapsed < profile.getBestTimeMillis()) {
				profile.setBestTimeMillis(elapsed);
			}
			profile.setTotalWins(profile.getTotalWins() + 1);
			profileStore.save(profile);
		} catch (Exception ignored) {
		}
	}

	private void broadcastMatch(Match match, String message) {
		Set<UUID> recipients = Set.copyOf(match.getLobbyPlayers());
		for (UUID id : recipients) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				Text.msg(player, message);
			}
		}
	}
}
