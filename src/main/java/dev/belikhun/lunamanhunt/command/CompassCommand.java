package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class CompassCommand implements TabExecutor {
	private static final String PERM_BASE = "lunamanhunt.compass";

	private final LobbyManager lobbyManager;
	private final CompassService compassService;

	public CompassCommand(LobbyManager lobbyManager, CompassService compassService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Chỉ hỗ trợ trong game.");
			return true;
		}

		if (!hasPerm(player, PERM_BASE)) {
			return true;
		}

		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return true;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Trận đấu chưa bắt đầu.");
			return true;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null && speedrunnerId.equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ " + UiColors.hunter("thợ săn") + " mới dùng lệnh này.");
			return true;
		}
		if (compassService.hasTrackingCompass(player)) {
			Text.msg(player, "<yellow>ℹ</yellow> Bạn đã có la bàn truy tìm.");
			return true;
		}

		compassService.giveTrackingCompass(player, match);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}

	private boolean hasPerm(Player player, String permission) {
		if (player.hasPermission(permission)) {
			return true;
		}
		Text.msg(player, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
		return false;
	}
}
