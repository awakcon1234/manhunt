package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class LmAdminCommand implements TabExecutor {
	private static final String PERM_BASE = "lunamanhunt.admin";
	private static final String PERM_FORCE_STOP = "lunamanhunt.admin.force-stop";
	private static final String PERM_RELOAD = "lunamanhunt.admin.reload";
	private static final String PERM_SET_SPAWN = "lunamanhunt.admin.setspawn";

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;

	public LmAdminCommand(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sendHelp(sender);
			return true;
		}

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "force-stop" -> handleForceStop(sender, args);
			case "reload" -> handleReload(sender);
			case "setspawn" -> handleSetSpawn(sender);
			case "help" -> sendHelp(sender);
			default -> sendHelp(sender);
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return filter(List.of("force-stop", "reload", "setspawn", "help"), args[0]);
		}
		if (args.length == 2 && "force-stop".equalsIgnoreCase(args[0])) {
			List<String> ids = matchManager.getMatches().stream()
				.map(Match::getId)
				.toList();
			return filter(ids, args[1]);
		}
		return List.of();
	}

	private void handleForceStop(CommandSender sender, String[] args) {
		if (!hasPerm(sender, PERM_FORCE_STOP)) {
			return;
		}
		Match match = null;
		if (args.length >= 2) {
			match = matchManager.getMatch(args[1]);
		} else if (sender instanceof Player player) {
			match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		}

		if (match == null) {
			reply(sender, "<red>❌</red> Không tìm thấy phòng cần dừng.");
			return;
		}

		matchManager.endMatch(match);
		reply(sender, "<green>✔</green> Đã dừng trận đấu <yellow>" + match.getId() + "</yellow>.");
	}

	private void handleReload(CommandSender sender) {
		if (!hasPerm(sender, PERM_RELOAD)) {
			return;
		}
		plugin.reloadConfig();
		reply(sender, "<green>✔</green> Đã tải lại cấu hình.");
	}

	private void handleSetSpawn(CommandSender sender) {
		if (!hasPerm(sender, PERM_SET_SPAWN)) {
			return;
		}
		if (!(sender instanceof Player player)) {
			reply(sender, "<red>❌</red> Chỉ hỗ trợ trong game.");
			return;
		}
		var loc = player.getLocation();
		plugin.getConfig().set("lobby.spawn.world", loc.getWorld() != null ? loc.getWorld().getName() : null);
		plugin.getConfig().set("lobby.spawn.x", loc.getX());
		plugin.getConfig().set("lobby.spawn.y", loc.getY());
		plugin.getConfig().set("lobby.spawn.z", loc.getZ());
		plugin.getConfig().set("lobby.spawn.yaw", loc.getYaw());
		plugin.getConfig().set("lobby.spawn.pitch", loc.getPitch());
		plugin.saveConfig();
		reply(sender, "<green>✔</green> Đã đặt điểm dịch chuyển sảnh.");
	}

	private void sendHelp(CommandSender sender) {
		reply(sender, "<yellow>●</yellow> <white>/lm force-stop [mã_phòng]</white>");
		reply(sender, "<yellow>●</yellow> <white>/lm reload</white>");
		reply(sender, "<yellow>●</yellow> <white>/lm setspawn</white>");
	}

	private boolean hasPerm(CommandSender sender, String permission) {
		if (sender.hasPermission(permission) || sender.hasPermission(PERM_BASE)) {
			return true;
		}
		reply(sender, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
		return false;
	}

	private void reply(CommandSender sender, String message) {
		if (sender instanceof Player player) {
			Text.msg(player, message);
		} else {
			sender.sendMessage(Text.colorize(message.replace("<red>", "&c").replace("</red>", "")
				.replace("<green>", "&a").replace("</green>", "")
				.replace("<yellow>", "&e").replace("</yellow>", "")
				.replace("<white>", "&f").replace("</white>", "")));
		}
	}

	private List<String> filter(List<String> options, String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return options;
		}
		String lower = prefix.toLowerCase(Locale.ROOT);
		return options.stream()
			.filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lower))
			.toList();
	}
}
