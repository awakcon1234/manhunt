package dev.belikhun.lunamanhunt.command;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyActionResult;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import org.bukkit.Sound;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MatchCommand implements TabExecutor {
	private static final String PERM_BASE = "lunamanhunt.match";
	private static final String PERM_CREATE = "lunamanhunt.match.create";
	private static final String PERM_JOIN = "lunamanhunt.match.join";
	private static final String PERM_INVITE = "lunamanhunt.match.invite";
	private static final String PERM_START = "lunamanhunt.match.start";
	private static final String PERM_LEAVE = "lunamanhunt.match.leave";
	private static final String PERM_HELP = "lunamanhunt.match.help";
	private static final String PERM_GUI = "lunamanhunt.match.gui";
	private static final String PERM_SEED = "lunamanhunt.match.seed";
	private static final String PERM_KIT = "lunamanhunt.match.kit";

	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final GuiService guiService;
	private final LobbyItemService lobbyItemService;

	public MatchCommand(LobbyManager lobbyManager, MatchManager matchManager, GuiService guiService,
			LobbyItemService lobbyItemService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.guiService = Objects.requireNonNull(guiService, "guiService");
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("Chỉ hỗ trợ trong game.");
			return true;
		}

		if (args.length == 0) {
			handleGui(player);
			return true;
		}

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "create" -> handleCreate(player);
			case "join" -> handleJoin(player, args);
			case "invite" -> handleInvite(player, args);
			case "accept" -> handleAccept(player);
			case "decline" -> handleDecline(player);
			case "start" -> handleStart(player);
			case "leave" -> handleLeave(player);
			case "seed" -> handleSeed(player, args);
			case "kit" -> handleKit(player, args);
			case "gui", "menu" -> handleGui(player);
			case "help" -> sendHelp(player);
			default -> Text.msg(player, "<red>❌</red> Lệnh không hợp lệ. Dùng "
				+ UiColors.command("/match help") + ".");
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			return filter(List.of("create", "join", "invite", "accept", "decline", "start", "leave", "seed", "kit", "gui", "menu", "help"), args[0]);
		}
		if (args.length == 2 && "join".equalsIgnoreCase(args[0]) && sender instanceof Player player) {
			List<String> ids = new ArrayList<>();
			for (Match match : matchManager.getMatches()) {
				if (match.getState() != MatchState.LOBBY) {
					continue;
				}
				if (canJoin(match, player.getUniqueId())) {
					ids.add(match.getId());
				}
			}
			return filter(ids, args[1]);
		}
		if (args.length == 2 && "invite".equalsIgnoreCase(args[0])) {
			List<String> names = new ArrayList<>();
			for (Player p : Bukkit.getOnlinePlayers()) {
				names.add(p.getName());
			}
			return filter(names, args[1]);
		}
		if (args.length == 2 && "seed".equalsIgnoreCase(args[0])) {
			return filter(List.of("xoa"), args[1]);
		}
		if (args.length == 2 && "kit".equalsIgnoreCase(args[0])) {
			return filter(List.of("xoa"), args[1]);
		}
		return List.of();
	}

	private void handleCreate(Player player) {
		if (!hasPerm(player, PERM_CREATE)) {
			return;
		}
		Match match = lobbyManager.createMatch(player.getUniqueId());
		Text.msg(player, "<green>✔</green> Đã tạo phòng " + UiColors.matchIdText(match.getId()) + ".");
		lobbyItemService.applyLobbyStateIfEligible(player);
		playSuccessSound(player);
	}

	private void handleJoin(Player player, String[] args) {
		if (!hasPerm(player, PERM_JOIN)) {
			return;
		}
		if (args.length < 2) {
			Text.msg(player, "<red>❌</red> Dùng: " + UiColors.command("/match join <mã_phòng>") + ".");
			return;
		}
		LobbyActionResult result = lobbyManager.join(player.getUniqueId(), args[1]);
		Text.msg(player, result.getMessage());
		if (result.isSuccess()) {
			lobbyItemService.applyLobbyStateIfEligible(player);
			playSuccessSound(player);
		}
	}

	private void handleInvite(Player player, String[] args) {
		if (!hasPerm(player, PERM_INVITE)) {
			return;
		}
		if (args.length < 2) {
			Text.msg(player, "<red>❌</red> Dùng: " + UiColors.command("/match invite <tên_người_chơi>") + ".");
			return;
		}
		Player target = Bukkit.getPlayer(args[1]);
		if (target == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy người chơi.");
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return;
		}
		LobbyActionResult result = lobbyManager.invite(player.getUniqueId(), target.getUniqueId(), match);
		Text.msg(player, result.getMessage());
		if (result.isSuccess()) {
			sendInviteMessage(target, match);
			playSuccessSound(player);
		}
	}

	private void handleAccept(Player player) {
		LobbyActionResult result = lobbyManager.acceptInvite(player.getUniqueId());
		Text.msg(player, result.getMessage());
		if (result.isSuccess()) {
			lobbyItemService.applyLobbyStateIfEligible(player);
			playSuccessSound(player);
		}
	}

	private void handleDecline(Player player) {
		LobbyActionResult result = lobbyManager.declineInvite(player.getUniqueId());
		Text.msg(player, result.getMessage());
		if (result.isSuccess()) {
			playSuccessSound(player);
		}
	}

	private void handleStart(Player player) {
		if (!hasPerm(player, PERM_START)) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được bắt đầu trận đấu.");
			return;
		}
		if (match.getState() != MatchState.LOBBY) {
			Text.msg(player, "<red>❌</red> Trận đấu đã bắt đầu hoặc đang tạo thế giới.");
			return;
		}
		boolean started = matchManager.startMatch(match);
		if (started) {
			Text.msg(player, "<yellow>⌛</yellow> Đang tạo thế giới cho trận đấu...");
			playSuccessSound(player);
			return;
		}
		Text.msg(player, "<red>❌</red> Không thể bắt đầu trận đấu lúc này.");
	}

	private void handleLeave(Player player) {
		if (!hasPerm(player, PERM_LEAVE)) {
			return;
		}
		LobbyActionResult result = lobbyManager.leave(player.getUniqueId());
		Text.msg(player, result.getMessage());
		lobbyItemService.applyLobbyStateIfEligible(player);
		if (result.isSuccess()) {
			playSuccessSound(player);
		}
	}

	private void handleSeed(Player player, String[] args) {
		if (!hasPerm(player, PERM_SEED)) {
			return;
		}
		Match match = requireOwnerLobby(player);
		if (match == null) {
			return;
		}
		if (args.length < 2) {
			Text.msg(player, "<red>❌</red> Dùng: " + UiColors.command("/match seed <giá_trị|xoa>") + ".");
			return;
		}
		String value = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length)).trim();
		if (value.equalsIgnoreCase("xoa")) {
			match.getSettings().setWorldSeed(null);
			Text.msg(player, "<green>✔</green> Đã xóa seed.");
			playSuccessSound(player);
			return;
		}
		match.getSettings().setWorldSeed(value);
		Text.msg(player, "<green>✔</green> Đã đặt seed.");
		playSuccessSound(player);
	}

	private void handleKit(Player player, String[] args) {
		if (!hasPerm(player, PERM_KIT)) {
			return;
		}
		Match match = requireOwnerLobby(player);
		if (match == null) {
			return;
		}
		if (args.length < 2) {
			Text.msg(player, "<red>❌</red> Dùng: " + UiColors.command("/match kit xoa") + ".");
			return;
		}
		if (!args[1].equalsIgnoreCase("xoa")) {
			Text.msg(player, "<red>❌</red> Dùng: " + UiColors.command("/match kit xoa") + ".");
			return;
		}
		match.getSettings().setSpeedrunnerKit(List.of());
		Text.msg(player, "<green>✔</green> Đã xóa bộ đồ " + UiColors.roleSpeedrunner() + ".");
		playSuccessSound(player);
	}

	private void playSuccessSound(Player player) {
		if (player == null) {
			return;
		}
		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);
	}

	private void sendHelp(Player player) {
		if (!hasPerm(player, PERM_HELP)) {
			return;
		}
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match create"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match join <mã_phòng>"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match invite <tên_người_chơi>"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match accept"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match decline"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match start"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match leave"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match seed <giá_trị|xoa>"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match kit xoa"));
		Text.msg(player, "<yellow>●</yellow> " + UiColors.command("/match gui"));
	}

	private void sendInviteMessage(Player target, Match match) {
		if (target == null || match == null) {
			return;
		}
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<green>✔</green> Bạn vừa nhận lời mời vào phòng " + UiColors.matchIdText(match.getId()) + "."));
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<gray>Chọn: </gray>"
			+ "<green><click:run_command:'/m accept'><hover:show_text:'<gray>Bấm để chấp nhận</gray>'><b>[CHẤP NHẬN]</b></hover></click></green> "
			+ "<red><click:run_command:'/m decline'><hover:show_text:'<gray>Bấm để từ chối</gray>'><b>[TỪ CHỐI]</b></hover></click></red> "
			+ "<gray>(</gray>" + UiColors.command("/m accept") + "<gray> | </gray>"
			+ UiColors.command("/m decline") + "<gray>)</gray>"));
		target.playSound(target.getLocation(), Sound.UI_TOAST_IN, 1.2f, 0.8f);
		target.playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 1.4f, 1.0f);
	}

	private void handleGui(Player player) {
		if (!hasPerm(player, PERM_GUI)) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match != null && match.getState() == MatchState.LOBBY) {
			guiService.openMatchInfoMenu(player);
			return;
		}
		guiService.openMainMenu(player);
	}

	private boolean hasPerm(Player player, String permission) {
		if (player.hasPermission(permission) || player.hasPermission(PERM_BASE)) {
			return true;
		}
		Text.msg(player, "<red>❌</red> Bạn không có quyền dùng lệnh này.");
		return false;
	}

	private Match requireOwnerLobby(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return null;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được chỉnh cài đặt.");
			return null;
		}
		if (match.getState() != MatchState.LOBBY) {
			Text.msg(player, "<red>❌</red> Chỉ chỉnh cài đặt khi đang ở sảnh.");
			return null;
		}
		return match;
	}

	private boolean canJoin(Match match, java.util.UUID playerId) {
		if (match.getOwnerId().equals(playerId)) {
			return true;
		}
		return switch (match.getSettings().getVisibility()) {
			case PUBLIC -> true;
			case INVITE_ONLY, PRIVATE -> match.getInvitedPlayers().contains(playerId);
		};
	}

	private List<String> filter(List<String> options, String prefix) {
		if (prefix == null || prefix.isEmpty()) {
			return options;
		}
		String lower = prefix.toLowerCase(Locale.ROOT);
		List<String> out = new ArrayList<>();
		for (String option : options) {
			if (option.toLowerCase(Locale.ROOT).startsWith(lower)) {
				out.add(option);
			}
		}
		return out;
	}
}
