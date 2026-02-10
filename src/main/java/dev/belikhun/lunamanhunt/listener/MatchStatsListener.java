package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchStats;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

import java.util.Objects;
import java.util.UUID;

public class MatchStatsListener implements Listener {
	private final LobbyManager lobbyManager;

	public MatchStatsListener(LobbyManager lobbyManager) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
	}

	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(player.getUniqueId())) {
			return;
		}
		World toWorld = player.getWorld();
		if (toWorld == null) {
			return;
		}
		long elapsed = resolveElapsed(match);
		MatchStats stats = match.getStats();
		if (toWorld.getEnvironment() == World.Environment.NETHER) {
			stats.setTimeToNetherMillis(elapsed);
		}
		if (toWorld.getEnvironment() == World.Environment.THE_END) {
			stats.setTimeToEndMillis(elapsed);
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event == null || event.getEntity() instanceof Player) {
			return;
		}
		Player killer = event.getEntity().getKiller();
		if (killer == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(killer.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		match.getStats().incrementMobKills();
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null) {
			return;
		}
		Player victim = event.getEntity();
		Match match = lobbyManager.getMatchForPlayer(victim.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		Player killer = victim.getKiller();
		if (killer == null || !match.getId().equals(lobbyManager.getMatchIdForPlayer(killer.getUniqueId()))) {
			match.getStats().incrementNaturalDeaths();
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null
				&& speedrunnerId.equals(killer.getUniqueId())
				&& match.getHunters().contains(victim.getUniqueId())) {
			match.getStats().incrementHuntersKilled();
		}
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event == null || !(event.getDamager() instanceof Player attacker)
				|| !(event.getEntity() instanceof Player victim)) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(attacker.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		if (!match.getId().equals(lobbyManager.getMatchIdForPlayer(victim.getUniqueId()))) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(attacker.getUniqueId())) {
			return;
		}
		if (!match.getHunters().contains(victim.getUniqueId())) {
			return;
		}
		match.getStats().addDamageToHunters(event.getFinalDamage());
	}

	private long resolveElapsed(Match match) {
		long startedAt = match.getStartedAtMillis() != null
			? match.getStartedAtMillis()
			: match.getCreatedAtMillis();
		return Math.max(0L, System.currentTimeMillis() - startedAt);
	}
}
