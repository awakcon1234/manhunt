package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.Objects;

public class EndPortalListener implements Listener {
	private final MatchEndService matchEndService;

	public EndPortalListener(MatchEndService matchEndService) {
		this.matchEndService = Objects.requireNonNull(matchEndService, "matchEndService");
	}

	@EventHandler
	public void onPortal(PlayerPortalEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		switch (event.getCause()) {
			case END_PORTAL -> matchEndService.handleSpeedrunnerPortal(event.getPlayer());
			default -> {
			}
		}
	}
}
