package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.WinConditionService;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;

public class MatchWinListener implements Listener {
	private final WinConditionService winConditionService;

	public MatchWinListener(WinConditionService winConditionService) {
		this.winConditionService = Objects.requireNonNull(winConditionService, "winConditionService");
	}

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null) {
			return;
		}
		Player victim = event.getEntity();
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
		Player killer = event.getEntity().getKiller();
		if (killer == null) {
			return;
		}
		winConditionService.handleDragonKill(killer);
	}
}
