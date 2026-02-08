package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Objects;

public class RunStartListener implements Listener {
	private final MatchLifecycleService matchLifecycleService;

	public RunStartListener(MatchLifecycleService matchLifecycleService) {
		this.matchLifecycleService = Objects.requireNonNull(matchLifecycleService, "matchLifecycleService");
	}

	@EventHandler
	public void onDamage(EntityDamageByEntityEvent event) {
		if (event == null) {
			return;
		}
		if (!(event.getDamager() instanceof Player attacker)) {
			return;
		}
		if (!(event.getEntity() instanceof Player victim)) {
			return;
		}
		if (matchLifecycleService.shouldCancelPendingDamage(attacker, victim)) {
			event.setCancelled(true);
			return;
		}
		matchLifecycleService.tryStartRun(attacker, victim);
	}
}
