package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BackCommand implements TabExecutor {
	private static final String PERM_BACK = "lunamanhunt.back";

	private final LobbyManager lobbyManager;
	private final PointService pointService;

	public BackCommand(LobbyManager lobbyManager, PointService pointService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Chỉ hỗ trợ trong game.");
			return true;
		}
		if (!player.hasPermission(PERM_BACK)) {
			Text.msg(player, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
			return true;
		}

		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Chỉ dùng được khi trận đang chạy.");
			return true;
		}

		UUID playerId = player.getUniqueId();
		if (!match.getHunters().contains(playerId)) {
			Text.msg(player, "<red>❌</red> Chỉ thợ săn mới dùng được lệnh này.");
			return true;
		}

		Location deathLocation = pointService.getHunterLastDeath(match, playerId);
		if (deathLocation == null || deathLocation.getWorld() == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa có vị trí chết trước đó.");
			return true;
		}

		int nextCost = pointService.getHunterBackCost(match, playerId);
		PointService.BackSpendResult spendResult = pointService.spendHunterBack(match, playerId);
		if (!spendResult.success()) {
			Text.msg(player, "<red>❌</red> Không đủ điểm để dùng " + UiColors.command("/back")
				+ ". Cần <white>" + pointService.formatPoints(nextCost) + "</white>, hiện có <white>"
				+ pointService.formatPoints(spendResult.remainingPoints()) + "</white>.");
			return true;
		}

		player.teleport(deathLocation);
		Text.msg(player, "<green>✔</green> Đã dịch chuyển về điểm chết trước đó. Trừ <white>"
			+ pointService.formatPoints(spendResult.cost()) + "</white>, còn <white>"
			+ pointService.formatPoints(spendResult.remainingPoints()) + "</white>.");
		Text.msg(player, "<yellow>ℹ</yellow> Lần dùng tiếp theo sẽ tốn <white>"
			+ pointService.formatPoints(pointService.getHunterBackCost(match, playerId)) + "</white>.");
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		return List.of();
	}
}
