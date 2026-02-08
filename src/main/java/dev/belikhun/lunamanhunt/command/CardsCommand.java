package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CardsCommand implements TabExecutor {
	private static final String PERM_BASE = "lunamanhunt.cards";
	private static final String PERM_USE = "lunamanhunt.cards.use";

	private final LobbyManager lobbyManager;
	private final CardService cardService;
	private final GuiService guiService;

	public CardsCommand(LobbyManager lobbyManager, CardService cardService, GuiService guiService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.guiService = Objects.requireNonNull(guiService, "guiService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Chỉ hỗ trợ trong game.");
			return true;
		}

		if (args.length == 0) {
			return handleList(player);
		}

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "use", "dung" -> {
				return handleUse(player, args);
			}
			case "draw", "rut" -> {
				return handleDrawInfo(player);
			}
			default -> {
				return handleList(player);
			}
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return List.of("use");
		}
		return List.of();
	}

	private boolean handleList(Player player) {
		if (!hasPerm(player, PERM_BASE)) {
			return true;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			guiService.openCardsInfoMenu(player, 0);
			return true;
		}
		if (match.getState() == MatchState.RUNNING
				&& match.getSpeedrunnerId() != null
				&& match.getSpeedrunnerId().equals(player.getUniqueId())) {
			guiService.openCardsMenu(player, match);
			return true;
		}
		guiService.openCardsInfoMenu(player, 0);
		return true;
	}

	private boolean handleUse(Player player, String[] args) {
		if (!hasPerm(player, PERM_USE)) {
			return true;
		}
		Match match = requireSpeedrunner(player);
		if (match == null) {
			return true;
		}
		if (args.length < 2) {
			Text.msg(player, "<red>❌</red> Dùng: <yellow>/cards use <số_thẻ></yellow>.");
			return true;
		}
		int index;
		try {
			index = Integer.parseInt(args[1]);
		} catch (NumberFormatException ex) {
			Text.msg(player, "<red>❌</red> Số thẻ không hợp lệ.");
			return true;
		}
		int maxSlots = cardService.getMaxSlots();
		if (index < 1 || index > maxSlots) {
			Text.msg(player, "<red>❌</red> Số thẻ không hợp lệ.");
			return true;
		}
		cardService.useKeptCard(player, match, index - 1);
		return true;
	}

	private boolean handleDrawInfo(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match != null && match.getState() == MatchState.RUNNING) {
			Text.msg(player, "<yellow>ℹ</yellow> Thẻ mới sẽ mở khi bạn đạt thành tựu.");
			guiService.openCardsMenu(player, match);
			return true;
		}
		Text.msg(player, "<yellow>ℹ</yellow> Thẻ mới sẽ mở khi bạn đạt thành tựu.");
		return true;
	}

	private Match requireSpeedrunner(Player player) {
		if (!hasPerm(player, PERM_BASE)) {
			return null;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return null;
		}
		if (match.getState() != MatchState.RUNNING) {
			Text.msg(player, "<red>❌</red> Trận đấu chưa bắt đầu.");
			return null;
		}
		if (match.getSpeedrunnerId() == null || !match.getSpeedrunnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ speedrunner mới dùng được thẻ.");
			return null;
		}
		return match;
	}

	private boolean hasPerm(Player player, String permission) {
		if (player.hasPermission(permission)) {
			return true;
		}
		Text.msg(player, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
		return false;
	}
}
