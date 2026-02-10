package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.World;

import java.util.Objects;
import java.util.logging.Logger;

public class EndPortalListener implements Listener {
	private final MatchEndService matchEndService;
	private final Logger logger;

	public EndPortalListener(MatchEndService matchEndService, Logger logger) {
		this.matchEndService = Objects.requireNonNull(matchEndService, "matchEndService");
		this.logger = Objects.requireNonNull(logger, "logger");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPortal(PlayerPortalEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		if (event.getFrom() != null && event.getFrom().getWorld() != null) {
			debug("Player portal event: player=" + event.getPlayer().getName()
				+ " cause=" + event.getCause()
				+ " from=" + event.getFrom().getWorld().getName()
				+ " cancelled=" + event.isCancelled());
		}
		switch (event.getCause()) {
			case END_PORTAL, END_GATEWAY -> {
				if (matchEndService.handleSpeedrunnerPortal(event.getPlayer())) {
					event.setCancelled(true);
					debug("Player portal cancelled for match end: player=" + event.getPlayer().getName()
						+ " cause=" + event.getCause());
				} else {
					debug("Player portal ignored (not speedrunner end): player=" + event.getPlayer().getName()
						+ " cause=" + event.getCause());
				}
			}
			default -> {
			}
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (event == null || event.getPlayer() == null) {
			return;
		}
		World fromWorld = event.getFrom() == null ? null : event.getFrom().getWorld();
		World toWorld = event.getTo() == null ? null : event.getTo().getWorld();
		if (fromWorld != null && toWorld != null) {
			boolean isEndRelated = fromWorld.getEnvironment() == World.Environment.THE_END
				|| toWorld.getEnvironment() == World.Environment.THE_END
				|| event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL
				|| event.getCause() == PlayerTeleportEvent.TeleportCause.END_GATEWAY;
			if (isEndRelated) {
				debug("Player teleport event: player=" + event.getPlayer().getName()
					+ " cause=" + event.getCause()
					+ " from=" + fromWorld.getName()
					+ " to=" + toWorld.getName()
					+ " cancelled=" + event.isCancelled());
			}
			if (fromWorld != null && toWorld != null
					&& fromWorld.getEnvironment() == World.Environment.THE_END
					&& toWorld.getEnvironment() == World.Environment.NORMAL) {
				if (matchEndService.handleSpeedrunnerPortal(event.getPlayer())) {
					event.setCancelled(true);
					debug("Player teleport cancelled for match end: player=" + event.getPlayer().getName()
						+ " from=" + fromWorld.getName() + " to=" + toWorld.getName());
				} else if (isEndRelated) {
					debug("Player teleport ignored (not speedrunner end): player=" + event.getPlayer().getName()
						+ " from=" + fromWorld.getName() + " to=" + toWorld.getName());
				}
				return;
			}
		}
		switch (event.getCause()) {
			case END_PORTAL, END_GATEWAY -> {
				if (matchEndService.handleSpeedrunnerPortal(event.getPlayer())) {
					event.setCancelled(true);
					debug("Player teleport cancelled for match end: player=" + event.getPlayer().getName()
						+ " cause=" + event.getCause());
				} else {
					debug("Player teleport ignored (not speedrunner end): player=" + event.getPlayer().getName()
						+ " cause=" + event.getCause());
				}
			}
			default -> {
			}
		}
	}

	private void debug(String message) {
		logger.info("[PortalDebug] " + message);
	}
}
