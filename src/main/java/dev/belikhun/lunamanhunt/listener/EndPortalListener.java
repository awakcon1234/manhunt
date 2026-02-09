package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.World;

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
			case END_GATEWAY -> matchEndService.handleSpeedrunnerPortal(event.getPlayer());
			default -> {
			}
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		if (event.getFrom() != null && event.getTo() != null) {
			World fromWorld = event.getFrom().getWorld();
			World toWorld = event.getTo().getWorld();
			if (fromWorld != null && toWorld != null
					&& fromWorld.getEnvironment() == World.Environment.THE_END
					&& toWorld.getEnvironment() == World.Environment.NORMAL) {
				matchEndService.handleSpeedrunnerPortal(event.getPlayer());
				return;
			}
		}
		switch (event.getCause()) {
			case END_PORTAL, END_GATEWAY -> matchEndService.handleSpeedrunnerPortal(event.getPlayer());
			default -> {
			}
		}
	}
}
