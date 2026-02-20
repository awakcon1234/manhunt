package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.WinConditionService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;
import java.util.UUID;

public class MatchWinListener implements Listener {
	private final WinConditionService winConditionService;
	private final LobbyManager lobbyManager;

	public MatchWinListener(WinConditionService winConditionService, LobbyManager lobbyManager) {
		this.winConditionService = Objects.requireNonNull(winConditionService, "winConditionService");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null) {
			return;
		}
		Player victim = event.getEntity();
		Match match = lobbyManager.getMatchForPlayer(victim.getUniqueId());
		if (match != null
			&& match.getState() == MatchState.RUNNING
			&& match.isSuddenDeathActive()
			&& match.getSpeedrunnerId() != null
			&& match.getSpeedrunnerId().equals(victim.getUniqueId())
			&& victim.getKiller() == null) {
			winConditionService.handleSuddenDeathNaturalSpeedrunnerDeath(victim, victim.getLocation());
			return;
		}
		Player killer = victim.getKiller();
		if (killer == null) {
			return;
		}
		winConditionService.handleHunterKill(killer, victim, victim.getLocation());
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event == null || !(event.getEntity() instanceof EnderDragon)) {
			return;
		}
		Match match = resolveRunningMatchForDragonWorld(event.getEntity().getWorld());
		if (match == null) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null) {
			return;
		}
		Player speedrunner = Bukkit.getPlayer(speedrunnerId);
		if (speedrunner == null) {
			return;
		}
		winConditionService.handleDragonKill(speedrunner);
	}

	private Match resolveRunningMatchForDragonWorld(World world) {
		if (world == null || lobbyManager == null) {
			return null;
		}
		String worldName = world.getName();
		for (Match match : lobbyManager.getActiveMatches()) {
			if (match == null || match.getState() != MatchState.RUNNING) {
				continue;
			}
			if (!isEndWorldOfMatch(match.getWorlds(), worldName)) {
				continue;
			}
			return match;
		}
		return null;
	}

	private boolean isEndWorldOfMatch(MatchWorlds worlds, String worldName) {
		if (worlds == null || worldName == null || worldName.isBlank()) {
			return false;
		}
		return worldName.equals(worlds.getEndName());
	}
}
