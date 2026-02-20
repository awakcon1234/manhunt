package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchLifecycleService;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.MatchEndService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Bukkit;
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
	private static final String PERM_CLOSE_MATCH = "lunamanhunt.admin.close-match";
	private static final String PERM_FORCE_STOP = "lunamanhunt.admin.force-stop";
	private static final String PERM_FORCE_JOIN_ALL = "lunamanhunt.admin.force-join-all";
	private static final String PERM_SUDDEN_DEATH = "lunamanhunt.admin.sudden-death";
	private static final String PERM_RELOAD = "lunamanhunt.admin.reload";
	private static final String PERM_SET_SPAWN = "lunamanhunt.admin.setspawn";

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final MatchLifecycleService matchLifecycleService;
	private final MatchEndService matchEndService;

	public LmAdminCommand(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager,
			MatchLifecycleService matchLifecycleService, MatchEndService matchEndService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.matchLifecycleService = Objects.requireNonNull(matchLifecycleService, "matchLifecycleService");
		this.matchEndService = Objects.requireNonNull(matchEndService, "matchEndService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			sendHelp(sender);
			return true;
		}

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "close-match" -> handleCloseMatch(sender, args);
			case "force-stop" -> handleForceStop(sender, args);
			case "force-join-all" -> handleForceJoinAll(sender, args);
			case "sudden-death" -> handleSuddenDeath(sender, args);
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
			return filter(List.of("close-match", "force-stop", "force-join-all", "sudden-death", "reload", "setspawn", "help"), args[0]);
		}
		if (args.length == 2 && ("force-stop".equalsIgnoreCase(args[0])
			|| "close-match".equalsIgnoreCase(args[0])
			|| "force-join-all".equalsIgnoreCase(args[0])
			|| "sudden-death".equalsIgnoreCase(args[0]))) {
			List<String> ids = matchManager.getMatches().stream()
				.map(Match::getId)
				.toList();
			return filter(ids, args[1]);
		}
		return List.of();
	}

	private void handleCloseMatch(CommandSender sender, String[] args) {
		if (!hasPerm(sender, PERM_CLOSE_MATCH)) {
			return;
		}
		if (args.length < 2 || args[1] == null || args[1].isBlank()) {
			reply(sender, "<red>❌</red> Dùng: <white>/lm close-match <mã_phòng></white>");
			return;
		}

		Match match = matchManager.getMatch(args[1]);
		if (match == null) {
			reply(sender, "<red>❌</red> Không tìm thấy phòng cần đóng.");
			return;
		}

		matchEndService.forceCleanup(match);
		reply(sender, "<green>✔</green> Đã đóng phòng " + UiColors.matchIdText(match.getId()) + ".");
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

		matchEndService.forceCleanup(match);
		reply(sender, "<green>✔</green> Đã dừng trận và dọn dẹp " + UiColors.matchIdText(match.getId()) + ".");
	}

	private void handleForceJoinAll(CommandSender sender, String[] args) {
		if (!hasPerm(sender, PERM_FORCE_JOIN_ALL)) {
			return;
		}
		if (args.length < 2 || args[1] == null || args[1].isBlank()) {
			reply(sender, "<red>❌</red> Dùng: <white>/lm force-join-all <mã_phòng></white>");
			return;
		}

		Match targetMatch = matchManager.getMatch(args[1]);
		if (targetMatch == null) {
			reply(sender, "<red>❌</red> Không tìm thấy phòng đích.");
			return;
		}
		if (targetMatch.getState() != MatchState.LOBBY) {
			reply(sender, "<red>❌</red> Chỉ có thể ép tất cả vào phòng đang ở sảnh.");
			return;
		}

		broadcastAll("<yellow>⚠</yellow> Quản trị viên đang gom tất cả người chơi vào phòng "
			+ UiColors.matchIdText(targetMatch.getId()) + "<gray>. Vui lòng chờ...</gray>");

		int closedCount = 0;
		for (Match match : java.util.Set.copyOf(matchManager.getMatches())) {
			if (match == null || match.getId().equals(targetMatch.getId())) {
				continue;
			}
			matchEndService.forceCleanup(match);
			closedCount++;
		}

		int joinedCount = 0;
		int failedCount = 0;
		for (Player online : Bukkit.getOnlinePlayers()) {
			var result = lobbyManager.forceJoin(online.getUniqueId(), targetMatch.getId());
			if (result.isSuccess()) {
				joinedCount++;
			} else {
				failedCount++;
			}
		}

		broadcastAll("<green>✔</green> Đã gom người chơi vào phòng " + UiColors.matchIdText(targetMatch.getId())
			+ "<gray>. Phòng khác đã được đóng: <white>" + closedCount + "</white></gray>");

		reply(sender, "<green>✔</green> Đã đóng <white>" + closedCount + "</white> phòng khác và ép <white>"
			+ joinedCount + "</white> người vào phòng " + UiColors.matchIdText(targetMatch.getId()) + ".");
		if (failedCount > 0) {
			reply(sender, "<yellow>ℹ</yellow> Có <white>" + failedCount + "</white> người không ép vào được.");
		}
	}

	private void handleSuddenDeath(CommandSender sender, String[] args) {
		if (!hasPerm(sender, PERM_SUDDEN_DEATH)) {
			return;
		}

		Match match = null;
		if (args.length >= 2 && args[1] != null && !args[1].isBlank()) {
			match = matchManager.getMatch(args[1]);
		} else if (sender instanceof Player player) {
			match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		} else {
			reply(sender, "<red>❌</red> Dùng: <white>/lm sudden-death <mã_phòng></white>");
			return;
		}

		if (match == null) {
			reply(sender, "<red>❌</red> Không tìm thấy phòng cần kích hoạt sudden death.");
			return;
		}
		if (match.getState() != MatchState.RUNNING) {
			reply(sender, "<red>❌</red> Chỉ kích hoạt được khi trận đang chạy.");
			return;
		}
		if (match.isSuddenDeathActive()) {
			reply(sender, "<yellow>ℹ</yellow> Phòng này đã ở trạng thái sudden death.");
			return;
		}

		if (!matchLifecycleService.forceSuddenDeath(match)) {
			reply(sender, "<red>❌</red> Không thể kích hoạt sudden death cho phòng này.");
			return;
		}

		reply(sender, "<green>✔</green> Đã kích hoạt <#ff6b6b><b>Sudden Death</b></#ff6b6b> cho phòng "
			+ UiColors.matchIdText(match.getId()) + ".");
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
		reply(sender, "<yellow>●</yellow> <white>/lm close-match <mã_phòng></white>");
		reply(sender, "<yellow>●</yellow> <white>/lm force-stop [mã_phòng]</white>");
		reply(sender, "<yellow>●</yellow> <white>/lm force-join-all <mã_phòng></white>");
		reply(sender, "<yellow>●</yellow> <white>/lm sudden-death [mã_phòng]</white>");
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

	private void broadcastAll(String message) {
		for (Player online : Bukkit.getOnlinePlayers()) {
			Text.msg(online, message);
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
