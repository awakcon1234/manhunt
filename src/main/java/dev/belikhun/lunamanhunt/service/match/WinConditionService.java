package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.profile.MatchHistoryEntry;
import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class WinConditionService {
	private final LobbyManager lobbyManager;
	private final ProfileStore profileStore;
	private final MatchEndService matchEndService;

	public WinConditionService(LobbyManager lobbyManager, ProfileStore profileStore,
			MatchEndService matchEndService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.profileStore = profileStore;
		this.matchEndService = Objects.requireNonNull(matchEndService, "matchEndService");
	}

	public boolean handleHunterKill(Player killer, Player victim, org.bukkit.Location deathLocation) {
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
		Text.msg(killer, "<green>✔</green> Bạn đã hạ Speedrunner!");
		killer.playSound(killer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.0f);
		Text.msg(victim, "<red>❌</red> Bạn đã bị Hunter hạ gục.");
		victim.playSound(victim.getLocation(), Sound.UI_TOAST_OUT, 0.7f, 1.0f);
		endWithHuntersByKill(match, deathLocation);
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
		Text.msg(killer, "<green>✔</green> Bạn đã hạ Ender Dragon! Hãy vào cổng End để kết thúc.");
		killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);
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

	private void endWithHuntersByKill(Match match, org.bukkit.Location deathLocation) {
		recordMatchHistory(match, MatchWinner.HUNTERS);
		matchEndService.endWithHunters(match, "<green>✔</green> Thợ săn đã chiến thắng!", deathLocation, true);
	}

	private void endWithHunters(Match match, String message) {
		recordMatchHistory(match, MatchWinner.HUNTERS);
		matchEndService.endWithHunters(match, message, null, false);
	}

	private void endWithSpeedrunner(Match match) {
		recordSpeedrunTime(match);
		recordMatchHistory(match, MatchWinner.SPEEDRUNNER);
		matchEndService.endWithSpeedrunner(match, "<green>✔</green> Speedrunner đã chiến thắng!");
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
			profileStore.save(profile);
		} catch (Exception ignored) {
		}
	}

	private void recordMatchHistory(Match match, MatchWinner winner) {
		if (profileStore == null || match == null || winner == null) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		Long startedAt = match.getStartedAtMillis();
		long start = startedAt == null ? match.getCreatedAtMillis() : startedAt;
		long endedAt = System.currentTimeMillis();
		long duration = Math.max(0L, endedAt - start);
		for (UUID playerId : Set.copyOf(match.getLobbyPlayers())) {
			boolean isSpeedrunner = speedrunnerId != null && speedrunnerId.equals(playerId);
			boolean won = winner == MatchWinner.SPEEDRUNNER ? isSpeedrunner : !isSpeedrunner;
			try {
				PlayerProfile profile = profileStore.load(playerId);
				MatchHistoryEntry entry = new MatchHistoryEntry();
				entry.setMatchId(match.getId());
				entry.setWinner(winner);
				entry.setRole(isSpeedrunner ? "SPEEDRUNNER" : "HUNTER");
				entry.setWon(won);
				entry.setStartedAtMillis(start);
				entry.setEndedAtMillis(endedAt);
				entry.setDurationMillis(duration);
				profile.getMatchHistory().add(entry);
				if (won) {
					profile.setTotalWins(profile.getTotalWins() + 1);
				} else {
					profile.setTotalLosses(profile.getTotalLosses() + 1);
				}
				profileStore.save(profile);
			} catch (Exception ignored) {
			}
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
