package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class PlayerDeathRoutingListener implements Listener {
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final LobbyTeleportService lobbyTeleportService;
	private final PointService pointService;

	public PlayerDeathRoutingListener(JavaPlugin plugin, LobbyManager lobbyManager,
			LobbyTeleportService lobbyTeleportService, PointService pointService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getEntity();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match != null
			&& match.getState() == MatchState.RUNNING
			&& match.getHunters().contains(player.getUniqueId())
			&& !match.isSuddenDeathActive()) {
			Location deathLocation = player.getLocation().clone();
			pointService.recordHunterDeath(match, player.getUniqueId(), deathLocation);
			sendHunterBackPrompt(player, match, deathLocation);
		}
		if (!isLobbyContext(match)) {
			return;
		}

		event.setKeepInventory(true);
		event.setKeepLevel(true);
		event.setDroppedExp(0);
		event.getDrops().clear();

		Bukkit.getScheduler().runTask(plugin, () -> {
			if (player.isOnline() && player.isDead()) {
				player.spigot().respawn();
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (isLobbyContext(match)) {
			event.setRespawnLocation(lobbyTeleportService.resolveLobbySpawn(player));
			return;
		}

		if (isSpeedrunnerEndingContext(match, player)) {
			Location endSpawn = resolveEndSpawn(match);
			if (endSpawn != null) {
				event.setRespawnLocation(endSpawn);
			}
			return;
		}

		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}

		if (match.isSuddenDeathActive() && match.getHunters().contains(player.getUniqueId())) {
			Location endSpawn = resolveEndSpawn(match);
			if (endSpawn != null) {
				event.setRespawnLocation(endSpawn);
			}
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (!player.isOnline()) {
					return;
				}
				player.setGameMode(org.bukkit.GameMode.SPECTATOR);
				Text.msg(player, "<red>❌</red> Bạn đã hết mạng trong <#ff6b6b><b>Sudden Death</b></#ff6b6b>.");
			});
			return;
		}

		Location current = event.getRespawnLocation();
		if (isMatchWorld(match, current == null ? null : current.getWorld())) {
			return;
		}

		Location deathWorldSpawn = null;
		World deathWorld = player.getWorld();
		if (isMatchWorld(match, deathWorld)) {
			deathWorldSpawn = deathWorld.getSpawnLocation();
		}
		if (deathWorldSpawn == null) {
			deathWorldSpawn = resolveOverworldSpawn(match);
		}
		if (deathWorldSpawn != null) {
			event.setRespawnLocation(deathWorldSpawn);
		}
	}

	private boolean isLobbyContext(Match match) {
		if (match == null || match.getState() == null) {
			return true;
		}
		return switch (match.getState()) {
			case LOBBY, WORLD_CREATING, WORLD_READY -> true;
			default -> false;
		};
	}

	private boolean isMatchWorld(Match match, World world) {
		if (match == null || match.getWorlds() == null || world == null) {
			return false;
		}
		String name = world.getName();
		MatchWorlds worlds = match.getWorlds();
		return name.equals(worlds.getOverworldName())
			|| name.equals(worlds.getNetherName())
			|| name.equals(worlds.getEndName());
	}

	private Location resolveOverworldSpawn(Match match) {
		if (match == null || match.getWorlds() == null) {
			return null;
		}
		World overworld = Bukkit.getWorld(match.getWorlds().getOverworldName());
		return overworld == null ? null : overworld.getSpawnLocation();
	}

	private Location resolveEndSpawn(Match match) {
		if (match == null || match.getWorlds() == null) {
			return null;
		}
		World endWorld = Bukkit.getWorld(match.getWorlds().getEndName());
		return endWorld == null ? null : endWorld.getSpawnLocation();
	}

	private boolean isSpeedrunnerEndingContext(Match match, Player player) {
		if (match == null || player == null) {
			return false;
		}
		if (match.getState() != MatchState.ENDING || match.getWinner() != MatchWinner.SPEEDRUNNER) {
			return false;
		}
		return match.getSpeedrunnerId() != null && match.getSpeedrunnerId().equals(player.getUniqueId());
	}

	private void sendHunterBackPrompt(Player player, Match match, Location deathLocation) {
		if (player == null || match == null || deathLocation == null) {
			return;
		}

		int x = deathLocation.getBlockX();
		int y = deathLocation.getBlockY();
		int z = deathLocation.getBlockZ();
		int cost = pointService.getHunterBackCost(match, player.getUniqueId());
		String coordPlain = "x: " + x + ", y: " + y + ", z: " + z;
		String coordColored = "<yellow>⚐</yellow> "
			+ "<gray>x:</gray> <#66ccff><bold>" + x + "</bold></#66ccff>"
			+ "<gray>,</gray> <gray>y:</gray> <#8aff92><bold>" + y + "</bold></#8aff92>"
			+ "<gray>,</gray> <gray>z:</gray> <#ffc36b><bold>" + z + "</bold></#ffc36b>";

		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (!player.isOnline()) {
				return;
			}

			Text.tell(player, "");
			Text.tell(player, "<gray>Bạn đã chết tại tọa độ </gray>"
				+ "<click:copy_to_clipboard:'" + coordPlain + "'><hover:show_text:'<yellow>Bấm để copy tọa độ</yellow>'>"
				+ coordColored + "</hover></click>");
			Text.tell(player, "<white>Dịch chuyển về chỗ bạn vừa chết không?</white>");
			Text.tell(player, "<click:run_command:'/back'><hover:show_text:'<yellow>Bấm để dịch chuyển về điểm chết</yellow>'><#66ccff><bold>[⏪ DỊCH CHUYỂN]</bold></#66ccff></hover></click> <gray>(/back)</gray>");
			Text.tell(player, "<yellow>ℹ</yellow> <gray>Dịch chuyển sẽ tốn <white>" + cost + " điểm</white>.</gray>");
		}, 2L);
	}
}
