package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.util.Debug;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.PortalType;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerPortalEvent;

import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class MatchPortalLinkListener implements Listener {
	private static final double NETHER_SCALE = 8.0;

	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final Logger logger;

	public MatchPortalLinkListener(LobbyManager lobbyManager, MatchManager matchManager, Logger logger) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.logger = Objects.requireNonNull(logger, "logger");
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onPlayerPortal(PlayerPortalEvent event) {
		if (event == null || event.isCancelled() || event.getFrom() == null) {
			return;
		}
		Player player = event.getPlayer();
		Location from = event.getFrom();
		World fromWorld = from.getWorld();
		if (fromWorld == null) {
			debug("Player portal ignored (missing world): player=" + player.getName()
				+ " cause=" + event.getCause());
			return;
		}
		debug("Player portal event: player=" + player.getName() + " cause=" + event.getCause()
			+ " from=" + fromWorld.getName());
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			debug("Player portal ignored (no match): player=" + player.getName()
				+ " from=" + fromWorld.getName());
			return;
		}
		PortalType type = resolvePortalType(event);
		if (type == null) {
			debug("Player portal ignored (unknown type): player=" + player.getName());
			return;
		}
		from = from.clone();
		debug("Player portal: player=" + player.getName() + " match=" + match.getId()
			+ " type=" + type + " from=" + fromWorld.getName());
		String linkedWorld = getLinkedWorld(match, fromWorld.getName(), type);
		Location newTo = getLinkedLocation(from, type, linkedWorld, player);
		if (newTo == null) {
			debug("Player portal cancelled: linkedWorld=" + linkedWorld + " from=" + fromWorld.getName());
			event.setCancelled(true);
			return;
		}
		event.setTo(newTo);

		World toWorld = newTo.getWorld();
		if (toWorld == null) {
			return;
		}
		if (fromWorld.getEnvironment() == World.Environment.THE_END && type == PortalType.ENDER) {
			Location bedSpawn = player.getRespawnLocation();
			if (bedSpawn != null && bedSpawn.getWorld() != null && bedSpawn.getWorld().getUID().equals(toWorld.getUID())) {
				event.setTo(bedSpawn);
			} else {
				event.setTo(toWorld.getSpawnLocation());
			}
		} else if (toWorld.getEnvironment() == World.Environment.THE_END && type == PortalType.ENDER) {
			Location spawnLocation = getEndPlatformLocation(player, toWorld);
			event.setTo(spawnLocation);
			createEndPlatform(toWorld);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityPortal(EntityPortalEvent event) {
		if (event == null || event.isCancelled() || event.getFrom() == null || event.getTo() == null) {
			return;
		}
		Location from = event.getFrom();
		Location originalTo = event.getTo();
		World fromWorld = from.getWorld();
		World originalToWorld = originalTo.getWorld();
		if (fromWorld == null || originalToWorld == null) {
			return;
		}
		debug("Entity portal event: entity=" + event.getEntity().getType()
			+ " from=" + fromWorld.getName() + " to=" + originalToWorld.getName());

		PortalType type = resolvePortalType(fromWorld, originalToWorld);
		if (type == null) {
			debug("Entity portal ignored (unknown type): entity=" + event.getEntity().getType());
			return;
		}
		Match match = findMatchByWorld(fromWorld.getName());
		if (match == null) {
			return;
		}
		debug("Entity portal: entity=" + event.getEntity().getType() + " match=" + match.getId()
			+ " type=" + type + " from=" + fromWorld.getName());
		String linkedWorld = getLinkedWorld(match, fromWorld.getName(), type);
		Location newTo = getLinkedLocation(from, type, linkedWorld, event.getEntity());
		if (newTo == null) {
			debug("Entity portal cancelled: linkedWorld=" + linkedWorld + " from=" + fromWorld.getName());
			event.setCancelled(true);
			return;
		}
		event.setTo(newTo);

		World toWorld = newTo.getWorld();
		if (toWorld == null) {
			return;
		}
		if (fromWorld.getEnvironment() == World.Environment.THE_END && type == PortalType.ENDER) {
			event.setTo(toWorld.getSpawnLocation());
		} else if (toWorld.getEnvironment() == World.Environment.THE_END && type == PortalType.ENDER) {
			Location spawnLocation = getEndPlatformLocation(event.getEntity(), toWorld);
			event.setTo(spawnLocation);
			createEndPlatform(toWorld);
		}
	}

	private PortalType resolvePortalType(PlayerPortalEvent event) {
		return switch (event.getCause()) {
			case END_PORTAL -> PortalType.ENDER;
			case NETHER_PORTAL -> PortalType.NETHER;
			default -> null;
		};
	}

	private PortalType resolvePortalType(World fromWorld, World toWorld) {
		if (toWorld.getEnvironment() == World.Environment.NETHER
				|| (fromWorld.getEnvironment() == World.Environment.NETHER
				&& toWorld.getEnvironment() == World.Environment.NORMAL)) {
			return PortalType.NETHER;
		}
		if (toWorld.getEnvironment() == World.Environment.THE_END
				|| (fromWorld.getEnvironment() == World.Environment.THE_END
				&& toWorld.getEnvironment() == World.Environment.NORMAL)) {
			return PortalType.ENDER;
		}
		return null;
	}

	private String getLinkedWorld(Match match, String currentWorld, PortalType type) {
		MatchWorlds worlds = match.getWorlds();
		if (worlds == null || currentWorld == null) {
			return null;
		}
		String overworldName = worlds.getOverworldName();
		String netherName = worlds.getNetherName();
		String endName = worlds.getEndName();
		if (type == PortalType.NETHER) {
			if (currentWorld.equalsIgnoreCase(overworldName)) {
				return netherName;
			}
			if (currentWorld.equalsIgnoreCase(netherName)) {
				return overworldName;
			}
		}
		if (type == PortalType.ENDER) {
			if (currentWorld.equalsIgnoreCase(overworldName)) {
				return endName;
			}
			if (currentWorld.equalsIgnoreCase(endName)) {
				return overworldName;
			}
		}
		return null;
	}

	private Location getLinkedLocation(Location currentLocation, PortalType type, String linkedWorld, Entity entity) {
		if (currentLocation == null || linkedWorld == null) {
			return null;
		}
		World fromWorld = currentLocation.getWorld();
		World targetWorld = Bukkit.getWorld(linkedWorld);
		if (fromWorld == null || targetWorld == null) {
			if (fromWorld == null) {
				return null;
			}
			World.Environment targetEnvironment = resolveTargetEnvironment(fromWorld, type);
			WorldCreator creator = new WorldCreator(linkedWorld).environment(targetEnvironment);
			targetWorld = Bukkit.createWorld(creator);
			debug("Loading target world: name=" + linkedWorld + " env=" + targetEnvironment
				+ " result=" + (targetWorld != null ? "ok" : "null"));
			if (targetWorld == null) {
				return null;
			}
		}
		if (fromWorld.getName().equalsIgnoreCase(linkedWorld)) {
			return null;
		}
		double scale = 1.0;
		if (type == PortalType.NETHER) {
			scale = fromWorld.getEnvironment() == World.Environment.NETHER ? NETHER_SCALE : 1.0 / NETHER_SCALE;
		}
		double targetX = currentLocation.getX() * scale;
		double targetZ = currentLocation.getZ() * scale;
		return new Location(targetWorld, targetX, currentLocation.getY(), targetZ,
			currentLocation.getYaw(), currentLocation.getPitch());
	}

	private World.Environment resolveTargetEnvironment(World fromWorld, PortalType type) {
		if (fromWorld == null || type == null) {
			return World.Environment.NORMAL;
		}
		if (type == PortalType.NETHER) {
			return fromWorld.getEnvironment() == World.Environment.NETHER
				? World.Environment.NORMAL
				: World.Environment.NETHER;
		}
		if (type == PortalType.ENDER) {
			return fromWorld.getEnvironment() == World.Environment.THE_END
				? World.Environment.NORMAL
				: World.Environment.THE_END;
		}
		return World.Environment.NORMAL;
	}

	private Location getEndPlatformLocation(Entity entity, World targetWorld) {
		if (entity instanceof Player) {
			return new Location(targetWorld, 100, 49, 0, 90, 0);
		}
		return new Location(targetWorld, 100.5, 50, 0.5, 90, 0);
	}

	private void createEndPlatform(World world) {
		if (world == null) {
			return;
		}
		Location center = new Location(world, 100, 49, 0);
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				world.getBlockAt(center.getBlockX() + x, center.getBlockY() - 1, center.getBlockZ() + z)
					.setType(Material.OBSIDIAN, false);
				for (int y = 1; y <= 3; y++) {
					world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z)
						.setType(Material.AIR, false);
				}
			}
		}
	}

	private void debug(String message) {
		if (!Debug.isEnabled()) {
			return;
		}
		logger.info("[PortalDebug] " + message);
	}

	private Match findMatchByWorld(String worldName) {
		if (worldName == null) {
			return null;
		}
		for (Match match : Set.copyOf(matchManager.getMatches())) {
			MatchWorlds worlds = match.getWorlds();
			if (worlds == null) {
				continue;
			}
			if (worldName.equalsIgnoreCase(worlds.getOverworldName())
					|| worldName.equalsIgnoreCase(worlds.getNetherName())
					|| worldName.equalsIgnoreCase(worlds.getEndName())) {
				return match;
			}
		}
		return null;
	}
}
