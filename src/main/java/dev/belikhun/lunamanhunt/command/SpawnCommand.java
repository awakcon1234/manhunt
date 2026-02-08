package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.service.spawn.SpawnService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;

public class SpawnCommand implements TabExecutor {
	private static final String PERM_SPAWN = "lunamanhunt.spawn";

	private final SpawnService spawnService;

	public SpawnCommand(SpawnService spawnService) {
		this.spawnService = Objects.requireNonNull(spawnService, "spawnService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Chỉ hỗ trợ trong game.");
			return true;
		}
		if (!player.hasPermission(PERM_SPAWN)) {
			Text.msg(player, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
			return true;
		}
		spawnService.handleCommand(player);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}
}
