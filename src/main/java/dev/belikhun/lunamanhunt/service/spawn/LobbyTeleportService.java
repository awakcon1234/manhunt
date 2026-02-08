package dev.belikhun.lunamanhunt.service.spawn;

import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public class LobbyTeleportService {
	private final JavaPlugin plugin;

	public LobbyTeleportService(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public void teleportToLobby(Player player, boolean announce) {
		if (player == null) {
			return;
		}
		Location spawn = resolveLobbySpawn(player);
		player.teleport(spawn);
		if (announce) {
			Text.msg(player, "<green>✔</green> Đã dịch chuyển về sảnh.");
		}
	}

	public Location resolveLobbySpawn(Player player) {
		String worldName = plugin.getConfig().getString("lobby.spawn.world", "");
		double x = plugin.getConfig().getDouble("lobby.spawn.x", Double.NaN);
		double y = plugin.getConfig().getDouble("lobby.spawn.y", Double.NaN);
		double z = plugin.getConfig().getDouble("lobby.spawn.z", Double.NaN);
		float yaw = (float) plugin.getConfig().getDouble("lobby.spawn.yaw", 0.0);
		float pitch = (float) plugin.getConfig().getDouble("lobby.spawn.pitch", 0.0);

		World world = worldName == null || worldName.isBlank() ? null : Bukkit.getWorld(worldName);
		if (world != null && !Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(z)) {
			return new Location(world, x, y, z, yaw, pitch);
		}

		List<World> worlds = Bukkit.getWorlds();
		if (!worlds.isEmpty()) {
			world = worlds.get(0);
		}
		if (world == null && player != null) {
			world = player.getWorld();
		}
		return world == null ? new Location(Bukkit.getWorlds().get(0), 0, 0, 0) : world.getSpawnLocation();
	}
}
