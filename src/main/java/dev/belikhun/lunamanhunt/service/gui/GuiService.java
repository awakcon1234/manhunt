package dev.belikhun.lunamanhunt.service.gui;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.model.card.CardDefinition;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyActionResult;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.util.ItemStackCodec;
import dev.belikhun.lunamanhunt.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class GuiService {
	private static final String ACTION_KEY = "lm_gui_action";
	private static final String TARGET_KEY = "lm_gui_target";

	private static final String ACTION_MAIN_CREATE = "main_create";
	private static final String ACTION_MAIN_MY = "main_my";
	private static final String ACTION_MAIN_SETTINGS = "main_settings";
	private static final String ACTION_MAIN_CARDS = "main_cards";
	private static final String ACTION_MAIN_REFRESH = "main_refresh";
	private static final String ACTION_MAIN_CLOSE = "main_close";

	private static final String ACTION_MATCH_JOIN = "match_join";

	private static final String ACTION_LOBBY_START = "lobby_start";
	private static final String ACTION_LOBBY_INVITE = "lobby_invite";
	private static final String ACTION_LOBBY_SETTINGS = "lobby_settings";
	private static final String ACTION_LOBBY_LEAVE = "lobby_leave";
	private static final String ACTION_LOBBY_BACK = "lobby_back";

	private static final String ACTION_INVITE_PLAYER = "invite_player";
	private static final String ACTION_INVITE_BACK = "invite_back";

	private static final String ACTION_SETTINGS_VISIBILITY = "settings_visibility";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_RANDOM = "settings_speedrunner_random";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_PICK = "settings_speedrunner_pick";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_ME = "settings_speedrunner_me";
	private static final String ACTION_SETTINGS_SEED = "settings_seed";
	private static final String ACTION_SETTINGS_KIT = "settings_kit";
	private static final String ACTION_SETTINGS_KIT_CLEAR = "settings_kit_clear";
	private static final String ACTION_SETTINGS_BORDER_DOWN = "settings_border_down";
	private static final String ACTION_SETTINGS_BORDER_UP = "settings_border_up";
	private static final String ACTION_SETTINGS_START_BORDER_DOWN = "settings_start_border_down";
	private static final String ACTION_SETTINGS_START_BORDER_UP = "settings_start_border_up";
	private static final String ACTION_SETTINGS_BACK = "settings_back";

	private static final String ACTION_SPEEDRUNNER_RANDOM = "speedrunner_random";
	private static final String ACTION_SPEEDRUNNER_PICK = "speedrunner_pick";
	private static final String ACTION_SPEEDRUNNER_BACK = "speedrunner_back";

	private static final String ACTION_KIT_CLEAR_CONFIRM = "kit_clear_confirm";
	private static final String ACTION_KIT_CLEAR_CANCEL = "kit_clear_cancel";

	private static final String ACTION_CARD_USE = "card_use";
	private static final String ACTION_CARD_CLOSE = "card_close";

	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final CardService cardService;
	private final PointService pointService;
	private final NamespacedKey actionKey;
	private final NamespacedKey targetKey;
	private final java.util.Map<UUID, String> seedInputs = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, String> kitEditors = new java.util.concurrent.ConcurrentHashMap<>();

	public GuiService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager, CardService cardService,
			PointService pointService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
		this.actionKey = new NamespacedKey(plugin, ACTION_KEY);
		this.targetKey = new NamespacedKey(plugin, TARGET_KEY);
	}

	public void openMainMenu(Player player) {
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.MAIN, null), 54,
			Text.mm("<gold>LunaManhunt</gold> <white>- Phòng đấu</white>"));
		fillMainMenu(player, inventory);
		player.openInventory(inventory);
	}

	public void openLobbyMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.LOBBY, match.getId()), 27,
			Text.mm("<gold>Phòng</gold> <white>" + match.getId() + "</white>"));
		fillLobbyMenu(player, match, inventory);
		player.openInventory(inventory);
	}

	public void openInviteMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.INVITE, match.getId()), 54,
			Text.mm("<gold>Mời người chơi</gold>"));
		fillInviteMenu(player, match, inventory);
		player.openInventory(inventory);
	}

	public void openSettingsMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.SETTINGS, match.getId()), 27,
			Text.mm("<gold>Cài đặt phòng</gold>"));
		fillSettingsMenu(match, inventory);
		player.openInventory(inventory);
	}

	public void openSpeedrunnerMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.SPEEDRUNNER, match.getId()), 54,
			Text.mm("<gold>Chọn speedrunner</gold>"));
		fillSpeedrunnerMenu(match, inventory);
		player.openInventory(inventory);
	}

	public void openKitEditor(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.KIT_EDITOR, match.getId()), 54,
			Text.mm("<gold>Bộ đồ speedrunner</gold>"));
		List<ItemStack> items = ItemStackCodec.decodeList(match.getSettings().getSpeedrunnerKit());
		for (int i = 0; i < inventory.getSize() && i < items.size(); i++) {
			inventory.setItem(i, items.get(i));
		}
		kitEditors.put(player.getUniqueId(), match.getId());
		player.openInventory(inventory);
		Text.msg(player, "<yellow>ℹ</yellow> Đặt vật phẩm vào khung, đóng lại để lưu.");
	}

	public void openKitClearConfirm(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.KIT_CLEAR_CONFIRM, match.getId()), 27,
			Text.mm("<red>Xác nhận xóa bộ đồ</red>"));
		fillKitClearConfirmMenu(inventory);
		player.openInventory(inventory);
	}

	public void openCardsMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.CARDS, match.getId()), 27,
			Text.mm("<gold>Thẻ bài</gold>"));
		fillCardsMenu(match, inventory);
		player.openInventory(inventory);
	}

	public boolean isGuiInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof GuiHolder;
	}

	public boolean handleMenuClick(Player player, Inventory inventory, ItemStack item) {
		if (player == null || inventory == null || item == null) {
			return false;
		}
		if (!(inventory.getHolder() instanceof GuiHolder holder)) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return true;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		String action = data.get(actionKey, PersistentDataType.STRING);
		String target = data.get(targetKey, PersistentDataType.STRING);
		if (action == null || action.isBlank()) {
			return true;
		}

			switch (action) {
			case ACTION_MAIN_CREATE -> handleCreate(player);
			case ACTION_MAIN_MY -> handleMyMatch(player);
			case ACTION_MAIN_SETTINGS -> Text.msg(player, "<yellow>ℹ</yellow> Cài đặt đang được phát triển.");
			case ACTION_MAIN_CARDS -> handleOpenCards(player);
			case ACTION_MAIN_REFRESH -> openMainMenu(player);
			case ACTION_MAIN_CLOSE -> player.closeInventory();
			case ACTION_MATCH_JOIN -> handleJoin(player, target);
			case ACTION_LOBBY_START -> handleStart(player, holder.matchId());
			case ACTION_LOBBY_INVITE -> handleOpenInvite(player, holder.matchId());
			case ACTION_LOBBY_SETTINGS -> handleOpenSettings(player, holder.matchId());
			case ACTION_LOBBY_LEAVE -> handleLeave(player, holder.matchId());
			case ACTION_LOBBY_BACK -> openMainMenu(player);
			case ACTION_INVITE_PLAYER -> handleInvitePlayer(player, holder.matchId(), target);
			case ACTION_INVITE_BACK -> handleBackToLobby(player, holder.matchId());
			case ACTION_SETTINGS_VISIBILITY -> handleVisibilityToggle(player, holder.matchId());
			case ACTION_SETTINGS_SPEEDRUNNER_RANDOM -> handleSpeedrunnerRandom(player, holder.matchId());
			case ACTION_SETTINGS_SPEEDRUNNER_PICK -> handleOpenSpeedrunnerMenu(player, holder.matchId());
			case ACTION_SETTINGS_SPEEDRUNNER_ME -> handleSpeedrunnerMe(player, holder.matchId());
			case ACTION_SETTINGS_SEED -> handleSeedPrompt(player, holder.matchId());
			case ACTION_SETTINGS_KIT -> handleOpenKitEditor(player, holder.matchId());
			case ACTION_SETTINGS_KIT_CLEAR -> handleOpenKitClearConfirm(player, holder.matchId());
			case ACTION_SETTINGS_BORDER_DOWN -> handleBorderChange(player, holder.matchId(), -100);
			case ACTION_SETTINGS_BORDER_UP -> handleBorderChange(player, holder.matchId(), 100);
			case ACTION_SETTINGS_START_BORDER_DOWN -> handleStartBorderChange(player, holder.matchId(), -1);
			case ACTION_SETTINGS_START_BORDER_UP -> handleStartBorderChange(player, holder.matchId(), 1);
			case ACTION_SETTINGS_BACK -> handleBackToLobby(player, holder.matchId());
			case ACTION_SPEEDRUNNER_RANDOM -> handleSpeedrunnerRandom(player, holder.matchId());
			case ACTION_SPEEDRUNNER_PICK -> handleSpeedrunnerPick(player, holder.matchId(), target);
			case ACTION_SPEEDRUNNER_BACK -> handleOpenSettings(player, holder.matchId());
			case ACTION_KIT_CLEAR_CONFIRM -> handleKitClear(player, holder.matchId());
			case ACTION_KIT_CLEAR_CANCEL -> handleOpenSettings(player, holder.matchId());
			case ACTION_CARD_USE -> handleUseCard(player, holder.matchId(), target);
			case ACTION_CARD_CLOSE -> player.closeInventory();
			default -> {
			}
		}
		return true;
	}

	private void fillMainMenu(Player player, Inventory inventory) {
		List<Match> matches = matchManager.getMatches().stream()
			.filter(match -> match.getState() == MatchState.LOBBY)
			.filter(match -> canJoin(match, player.getUniqueId()))
			.sorted(Comparator.comparing(Match::getId))
			.toList();

		int slot = 0;
		for (Match match : matches) {
			if (slot >= 45) {
				break;
			}
			inventory.setItem(slot++, buildMatchItem(match));
		}

		inventory.setItem(45, buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Tạo phòng",
			List.of("<gray>Tạo phòng mới và trở thành chủ phòng.</gray>"), ACTION_MAIN_CREATE, null));

		inventory.setItem(47, buildActionItem(Material.COMPARATOR,
			"<yellow>ℹ</yellow> Cài đặt",
			List.of("<gray>Thiết lập cá nhân (đang phát triển).</gray>"), ACTION_MAIN_SETTINGS, null));

		inventory.setItem(48, buildActionItem(Material.EMERALD,
			"<yellow>ℹ</yellow> Thẻ bài",
			List.of("<gray>Xem thẻ bài đã rút.</gray>"), ACTION_MAIN_CARDS, null));

		inventory.setItem(49, buildActionItem(Material.CLOCK,
			"<yellow>ℹ</yellow> Làm mới",
			List.of("<gray>Cập nhật danh sách phòng.</gray>"), ACTION_MAIN_REFRESH, null));

		inventory.setItem(53, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng bảng chọn.</gray>"), ACTION_MAIN_CLOSE, null));
	}

	private void fillLobbyMenu(Player player, Match match, Inventory inventory) {
		inventory.setItem(4, buildInfoItem(Material.BOOK,
			"<yellow>ℹ</yellow> Thông tin phòng",
			List.of(
				"<gray>Mã phòng: <white>" + match.getId() + "</white></gray>",
				"<gray>Trạng thái: <white>" + formatState(match.getState()) + "</white></gray>",
				"<gray>Người chơi: <white>" + match.getLobbyPlayers().size() + "</white></gray>"
			)));

		inventory.setItem(10, buildActionItem(Material.COMPARATOR,
			"<yellow>ℹ</yellow> Cài đặt",
			List.of("<gray>Chỉnh cấu hình phòng.</gray>"), ACTION_LOBBY_SETTINGS, null));

		inventory.setItem(12, buildActionItem(Material.PAPER,
			"<yellow>ℹ</yellow> Mời người chơi",
			List.of("<gray>Mời người chơi vào phòng.</gray>"), ACTION_LOBBY_INVITE, null));

		boolean isOwner = match.getOwnerId().equals(player.getUniqueId());
		if (isOwner) {
			inventory.setItem(14, buildActionItem(Material.LIME_WOOL,
				"<green>✔</green> Bắt đầu",
				List.of("<gray>Bắt đầu trận đấu.</gray>"), ACTION_LOBBY_START, null));
		} else {
			inventory.setItem(14, buildInfoItem(Material.GRAY_WOOL,
				"<yellow>ℹ</yellow> Chờ chủ phòng",
				List.of("<gray>Chỉ chủ phòng mới được bắt đầu.</gray>")));
		}

		inventory.setItem(16, buildActionItem(Material.RED_WOOL,
			"<red>❌</red> Rời phòng",
			List.of("<gray>Rời khỏi phòng hiện tại.</gray>"), ACTION_LOBBY_LEAVE, null));

		inventory.setItem(22, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về danh sách phòng.</gray>"), ACTION_LOBBY_BACK, null));
	}

	private void fillInviteMenu(Player player, Match match, Inventory inventory) {
		List<Player> candidates = new ArrayList<>();
		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online.getUniqueId().equals(player.getUniqueId())) {
				continue;
			}
			if (match.getLobbyPlayers().contains(online.getUniqueId())) {
				continue;
			}
			if (match.getInvitedPlayers().contains(online.getUniqueId())) {
				continue;
			}
			if (lobbyManager.getMatchForPlayer(online.getUniqueId()) != null) {
				continue;
			}
			candidates.add(online);
		}

		int slot = 0;
		for (Player target : candidates) {
			if (slot >= 45) {
				break;
			}
			inventory.setItem(slot++, buildPlayerItem(target));
		}

		if (candidates.isEmpty()) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Không có người chơi",
				List.of("<gray>Không có ai để mời lúc này.</gray>")));
		}

		inventory.setItem(49, buildActionItem(Material.CLOCK,
			"<yellow>ℹ</yellow> Làm mới",
			List.of("<gray>Cập nhật danh sách.</gray>"), ACTION_LOBBY_INVITE, null));

		inventory.setItem(53, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về phòng.</gray>"), ACTION_INVITE_BACK, null));
	}

	private void fillSettingsMenu(Match match, Inventory inventory) {
		MatchSettings settings = match.getSettings();
		String seedText = settings.getWorldSeed() == null || settings.getWorldSeed().isBlank()
			? "Chưa đặt"
			: settings.getWorldSeed();
		int kitCount = ItemStackCodec.decodeList(settings.getSpeedrunnerKit()).size();

		inventory.setItem(4, buildInfoItem(Material.BOOK,
			"<yellow>ℹ</yellow> Thông tin cài đặt",
			List.of(
				"<gray>Hiển thị: <white>" + formatVisibility(settings.getVisibility()) + "</white></gray>",
				"<gray>Speedrunner: <white>" + formatSpeedrunner(settings) + "</white></gray>",
				"<gray>Viền tối đa: <white>" + settings.getWorldBorderLimit() + "</white></gray>",
				"<gray>Viền ban đầu: <white>" + settings.getStartingBorder() + "</white></gray>",
				"<gray>Seed: <white>" + seedText + "</white></gray>"
			)));

		inventory.setItem(10, buildActionItem(Material.NAME_TAG,
			"<yellow>ℹ</yellow> Hiển thị",
			List.of("<gray>Bấm để đổi chế độ hiển thị.</gray>"), ACTION_SETTINGS_VISIBILITY, null));

		inventory.setItem(12, buildActionItem(Material.COMPASS,
			"<yellow>ℹ</yellow> Speedrunner ngẫu nhiên",
			List.of("<gray>Bấm để chọn ngẫu nhiên.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_RANDOM, null));

		inventory.setItem(13, buildActionItem(Material.PLAYER_HEAD,
			"<yellow>ℹ</yellow> Chọn speedrunner",
			List.of("<gray>Bấm để chọn người chơi.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_PICK, null));

		inventory.setItem(14, buildActionItem(Material.PLAYER_HEAD,
			"<yellow>ℹ</yellow> Speedrunner là bạn",
			List.of("<gray>Bấm để chọn chính bạn.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_ME, null));

		inventory.setItem(16, buildActionItem(Material.PAPER,
			"<yellow>ℹ</yellow> Seed",
			List.of("<gray>Bấm để nhập seed bằng chat.</gray>"), ACTION_SETTINGS_SEED, null));

		inventory.setItem(15, buildActionItem(Material.CHEST,
			"<yellow>ℹ</yellow> Bộ đồ speedrunner",
			List.of("<gray>Số vật phẩm: <white>" + kitCount + "</white></gray>",
				"<gray>Bấm để chỉnh.</gray>"), ACTION_SETTINGS_KIT, null));

		inventory.setItem(17, buildActionItem(Material.BARRIER,
			"<red>❌</red> Xóa bộ đồ",
			List.of("<gray>Đặt bộ đồ về trống.</gray>"), ACTION_SETTINGS_KIT_CLEAR, null));

		inventory.setItem(20, buildActionItem(Material.RED_WOOL,
			"<red>❌</red> Giảm viền tối đa",
			List.of("<gray>Giảm 100 block.</gray>"), ACTION_SETTINGS_BORDER_DOWN, null));

		inventory.setItem(22, buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Tăng viền tối đa",
			List.of("<gray>Tăng 100 block.</gray>"), ACTION_SETTINGS_BORDER_UP, null));

		inventory.setItem(24, buildActionItem(Material.RED_WOOL,
			"<red>❌</red> Giảm viền ban đầu",
			List.of("<gray>Giảm 1 block.</gray>"), ACTION_SETTINGS_START_BORDER_DOWN, null));

		inventory.setItem(26, buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Tăng viền ban đầu",
			List.of("<gray>Tăng 1 block.</gray>"), ACTION_SETTINGS_START_BORDER_UP, null));

		inventory.setItem(18, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về phòng.</gray>"), ACTION_SETTINGS_BACK, null));
	}

	private void fillKitClearConfirmMenu(Inventory inventory) {
		inventory.setItem(11, buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Xác nhận",
			List.of("<gray>Xóa toàn bộ bộ đồ speedrunner.</gray>"), ACTION_KIT_CLEAR_CONFIRM, null));

		inventory.setItem(13, buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Lưu ý",
			List.of("<gray>Thao tác này không thể hoàn tác.</gray>")));

		inventory.setItem(15, buildActionItem(Material.RED_WOOL,
			"<red>❌</red> Hủy",
			List.of("<gray>Quay về cài đặt.</gray>"), ACTION_KIT_CLEAR_CANCEL, null));
	}

	private void fillCardsMenu(Match match, Inventory inventory) {
		int points = match.getSpeedrunnerPoints();
		List<CardDefinition> cards = cardService.getDrawnCards(match);
		inventory.setItem(4, buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Điểm hiện tại",
			List.of("<gray>Bạn có <white>" + pointService.formatPoints(points) + "</white>.</gray>")));

		int[] slots = new int[] { 11, 13, 15 };
		for (int i = 0; i < slots.length; i++) {
			if (i >= cards.size()) {
				break;
			}
			CardDefinition card = cards.get(i);
			inventory.setItem(slots[i], cardService.buildCardItem(card, i, actionKey, targetKey, ACTION_CARD_USE));
		}

		if (cards.isEmpty()) {
			inventory.setItem(13, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Chưa có thẻ",
				List.of("<gray>Nhận thành tựu để rút thẻ.</gray>")));
		}

		inventory.setItem(26, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng bảng thẻ bài.</gray>"), ACTION_CARD_CLOSE, null));
	}

	private void fillSpeedrunnerMenu(Match match, Inventory inventory) {
		List<UUID> members = new ArrayList<>(match.getLobbyPlayers());
		int slot = 0;
		for (UUID id : members) {
			if (slot >= 45) {
				break;
			}
			Player player = Bukkit.getPlayer(id);
			if (player == null) {
				continue;
			}
			inventory.setItem(slot++, buildSpeedrunnerItem(player));
		}

		if (slot == 0) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Không có người chơi",
				List.of("<gray>Không có ai trong phòng.</gray>")));
		}

		inventory.setItem(45, buildActionItem(Material.COMPASS,
			"<yellow>ℹ</yellow> Ngẫu nhiên",
			List.of("<gray>Chọn speedrunner ngẫu nhiên.</gray>"), ACTION_SPEEDRUNNER_RANDOM, null));

		inventory.setItem(53, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về cài đặt.</gray>"), ACTION_SPEEDRUNNER_BACK, null));
	}

	private void handleCreate(Player player) {
		if (lobbyManager.getMatchForPlayer(player.getUniqueId()) != null) {
			Text.msg(player, "<red>❌</red> Bạn đang ở trong một phòng khác.");
			return;
		}
		Match match = lobbyManager.createMatch(player.getUniqueId());
		Text.msg(player, "<green>✔</green> Đã tạo phòng <yellow>" + match.getId() + "</yellow>.");
		openLobbyMenu(player, match);
	}

	private void handleMyMatch(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return;
		}
		openLobbyMenu(player, match);
	}

	private void handleJoin(Player player, String matchId) {
		if (matchId == null || matchId.isBlank()) {
			return;
		}
		LobbyActionResult result = lobbyManager.join(player.getUniqueId(), matchId);
		Text.msg(player, result.getMessage());
		if (result.isSuccess() && result.getMatch() != null) {
			openLobbyMenu(player, result.getMatch());
		}
	}

	private void handleStart(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
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
			Text.msg(player, "<green>✔</green> Đang tạo thế giới cho trận đấu...");
			return;
		}
		Text.msg(player, "<red>❌</red> Không thể bắt đầu trận đấu lúc này.");
	}

	private void handleOpenInvite(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được mời người chơi.");
			return;
		}
		openInviteMenu(player, match);
	}

	private void handleInvitePlayer(Player player, String matchId, String targetId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được mời người chơi.");
			return;
		}
		if (targetId == null || targetId.isBlank()) {
			return;
		}
		UUID targetUuid;
		try {
			targetUuid = UUID.fromString(targetId);
		} catch (IllegalArgumentException ex) {
			return;
		}
		Player target = Bukkit.getPlayer(targetUuid);
		if (target == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy người chơi.");
			return;
		}
		LobbyActionResult result = lobbyManager.invite(player.getUniqueId(), target.getUniqueId(), match);
		Text.msg(player, result.getMessage());
		if (result.isSuccess()) {
			Text.msg(target, "<green>✔</green> Bạn vừa nhận lời mời vào phòng <yellow>" + match.getId() + "</yellow>.");
		}
		openInviteMenu(player, match);
	}

	private void handleOpenSettings(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được chỉnh cài đặt.");
			return;
		}
		if (match.getState() != MatchState.LOBBY) {
			Text.msg(player, "<red>❌</red> Chỉ chỉnh cài đặt khi đang ở sảnh.");
			return;
		}
		openSettingsMenu(player, match);
	}

	private void handleOpenCards(Player player) {
		Match match = requireSpeedrunnerMatch(player);
		if (match == null) {
			return;
		}
		openCardsMenu(player, match);
	}

	private void handleOpenSpeedrunnerMenu(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		openSpeedrunnerMenu(player, match);
	}

	private void handleBackToLobby(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			openMainMenu(player);
			return;
		}
		openLobbyMenu(player, match);
	}

	private void handleVisibilityToggle(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchVisibility next = switch (match.getSettings().getVisibility()) {
			case PUBLIC -> MatchVisibility.INVITE_ONLY;
			case INVITE_ONLY -> MatchVisibility.PRIVATE;
			case PRIVATE -> MatchVisibility.PUBLIC;
		};
		match.getSettings().setVisibility(next);
		Text.msg(player, "<green>✔</green> Đã đổi hiển thị sang <yellow>" + formatVisibility(next) + "</yellow>.");
		openSettingsMenu(player, match);
	}

	private void handleSpeedrunnerRandom(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setSpeedrunnerSelection(SpeedrunnerSelection.RANDOM);
		match.getSettings().setSpeedrunnerId(null);
		Text.msg(player, "<green>✔</green> Đã chọn speedrunner ngẫu nhiên.");
		openSettingsMenu(player, match);
	}

	private void handleSpeedrunnerMe(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setSpeedrunnerSelection(SpeedrunnerSelection.SPECIFIC);
		match.getSettings().setSpeedrunnerId(player.getUniqueId());
		Text.msg(player, "<green>✔</green> Đã chọn bạn làm speedrunner.");
		openSettingsMenu(player, match);
	}

	private void handleSpeedrunnerPick(Player player, String matchId, String targetId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null || targetId == null || targetId.isBlank()) {
			return;
		}
		UUID targetUuid;
		try {
			targetUuid = UUID.fromString(targetId);
		} catch (IllegalArgumentException ex) {
			return;
		}
		if (!match.getLobbyPlayers().contains(targetUuid)) {
			Text.msg(player, "<red>❌</red> Người chơi không ở trong phòng.");
			return;
		}
		match.getSettings().setSpeedrunnerSelection(SpeedrunnerSelection.SPECIFIC);
		match.getSettings().setSpeedrunnerId(targetUuid);
		Text.msg(player, "<green>✔</green> Đã chọn speedrunner.");
		openSpeedrunnerMenu(player, match);
	}

	private void handleSeedPrompt(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		seedInputs.put(player.getUniqueId(), match.getId());
		player.closeInventory();
		Text.msg(player, "<yellow>ℹ</yellow> Nhập seed vào chat. Gõ <white>huy</white> để hủy, <white>xoa</white> để xóa.");
	}

	private void handleOpenKitEditor(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		openKitEditor(player, match);
	}

	private void handleOpenKitClearConfirm(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		openKitClearConfirm(player, match);
	}

	private void handleKitClear(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setSpeedrunnerKit(List.of());
		Text.msg(player, "<green>✔</green> Đã xóa bộ đồ speedrunner.");
		openSettingsMenu(player, match);
	}

	private void handleUseCard(Player player, String matchId, String indexText) {
		Match match = requireSpeedrunnerMatch(player);
		if (match == null || indexText == null || indexText.isBlank()) {
			return;
		}
		int index;
		try {
			index = Integer.parseInt(indexText);
		} catch (NumberFormatException ex) {
			return;
		}
		List<CardDefinition> cards = match.getDrawnCards();
		if (index < 0 || index >= cards.size()) {
			return;
		}
		CardDefinition card = cards.get(index);
		if (!cardService.canUseCard(match, card)) {
			Text.msg(player, "<red>❌</red> Bạn không đủ điểm để dùng thẻ này.");
			openCardsMenu(player, match);
			return;
		}
		cardService.useCard(player, match, card);
		openCardsMenu(player, match);
	}

	private void handleBorderChange(Player player, String matchId, int delta) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings settings = match.getSettings();
		int current = settings.getWorldBorderLimit();
		int next = clamp(current + delta, 100, 100000);
		settings.setWorldBorderLimit(next);
		if (settings.getStartingBorder() > next) {
			settings.setStartingBorder(next);
		}
		Text.msg(player, "<green>✔</green> Viền tối đa: <yellow>" + next + "</yellow>.");
		openSettingsMenu(player, match);
	}

	private void handleStartBorderChange(Player player, String matchId, int delta) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings settings = match.getSettings();
		int current = settings.getStartingBorder();
		int max = Math.max(5, settings.getWorldBorderLimit());
		int next = clamp(current + delta, 5, max);
		settings.setStartingBorder(next);
		Text.msg(player, "<green>✔</green> Viền ban đầu: <yellow>" + next + "</yellow>.");
		openSettingsMenu(player, match);
	}

	public boolean hasSeedInput(Player player) {
		return player != null && seedInputs.containsKey(player.getUniqueId());
	}

	public void handleSeedInput(Player player, String message) {
		if (player == null) {
			return;
		}
		String matchId = seedInputs.remove(player.getUniqueId());
		if (matchId == null) {
			return;
		}
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		String input = message == null ? "" : message.trim();
		if (input.equalsIgnoreCase("huy")) {
			Text.msg(player, "<yellow>ℹ</yellow> Đã hủy nhập seed.");
			openSettingsMenu(player, match);
			return;
		}
		if (input.equalsIgnoreCase("xoa")) {
			match.getSettings().setWorldSeed(null);
			Text.msg(player, "<green>✔</green> Đã xóa seed.");
			openSettingsMenu(player, match);
			return;
		}
		match.getSettings().setWorldSeed(input);
		Text.msg(player, "<green>✔</green> Đã đặt seed.");
		openSettingsMenu(player, match);
	}

	public boolean isKitEditorInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof GuiHolder holder
			&& holder.type() == GuiType.KIT_EDITOR;
	}

	public void handleKitClose(Player player, Inventory inventory) {
		if (player == null || inventory == null) {
			return;
		}
		String matchId = kitEditors.remove(player.getUniqueId());
		if (matchId == null) {
			return;
		}
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		List<ItemStack> items = new ArrayList<>();
		for (ItemStack item : inventory.getContents()) {
			if (item == null || item.getType() == Material.AIR) {
				continue;
			}
			items.add(item.clone());
		}
		match.getSettings().setSpeedrunnerKit(ItemStackCodec.encodeList(items));
		Text.msg(player, "<green>✔</green> Đã lưu bộ đồ speedrunner.");
		openSettingsMenu(player, match);
	}

	private Match requireSpeedrunnerMatch(Player player) {
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

	private void handleLeave(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		LobbyActionResult result = lobbyManager.leave(player.getUniqueId());
		Text.msg(player, result.getMessage());
		if (match != null && match.getOwnerId().equals(player.getUniqueId())) {
			openMainMenu(player);
			return;
		}
		openMainMenu(player);
	}

	private String formatState(MatchState state) {
		if (state == null) {
			return "Không rõ";
		}
		return switch (state) {
			case LOBBY -> "Chờ";
			case WORLD_CREATING -> "Tạo thế giới";
			case WORLD_READY -> "Sẵn sàng";
			case RUN_PENDING -> "Chờ bắt đầu";
			case RUNNING -> "Đang chạy";
			case ENDING -> "Kết thúc";
			case CLEANUP -> "Dọn dẹp";
			case FINISHED -> "Đã xong";
		};
	}

	private String formatVisibility(MatchVisibility visibility) {
		if (visibility == null) {
			return "Không rõ";
		}
		return switch (visibility) {
			case PUBLIC -> "Công khai";
			case INVITE_ONLY -> "Chỉ mời";
			case PRIVATE -> "Riêng tư";
		};
	}

	private String formatSpeedrunner(MatchSettings settings) {
		if (settings == null) {
			return "Chưa đặt";
		}
		if (settings.getSpeedrunnerSelection() == SpeedrunnerSelection.RANDOM) {
			return "Ngẫu nhiên";
		}
		UUID id = settings.getSpeedrunnerId();
		if (id == null) {
			return "Chưa đặt";
		}
		Player player = Bukkit.getPlayer(id);
		return player != null ? player.getName() : "Người chơi";
	}

	private Match requireSettingsMatch(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
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

	private int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private boolean canJoin(Match match, UUID playerId) {
		if (match.getOwnerId().equals(playerId)) {
			return true;
		}
		MatchVisibility visibility = match.getSettings().getVisibility();
		if (visibility == MatchVisibility.PUBLIC) {
			return true;
		}
		return match.getInvitedPlayers().contains(playerId);
	}

	private ItemStack buildMatchItem(Match match) {
		Material material = switch (match.getSettings().getVisibility()) {
			case PUBLIC -> Material.LIME_CONCRETE;
			case INVITE_ONLY -> Material.YELLOW_CONCRETE;
			case PRIVATE -> Material.RED_CONCRETE;
		};

		String ownerName = "Không rõ";
		Player owner = Bukkit.getPlayer(match.getOwnerId());
		if (owner != null) {
			ownerName = owner.getName();
		}

		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>Mã phòng: <white>" + match.getId() + "</white></yellow>"));
			meta.lore(mmNoItalic(List.of(
				"<gray>Chủ phòng: <white>" + ownerName + "</white></gray>",
				"<gray>Hiển thị: <white>" + formatVisibility(match.getSettings().getVisibility()) + "</white></gray>",
				"<gray>Người chơi: <white>" + match.getLobbyPlayers().size() + "</white></gray>",
				"<gray>Bấm để tham gia.</gray>"
			)));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MATCH_JOIN);
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, match.getId());
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildPlayerItem(Player target) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>" + target.getName() + "</yellow>"));
			meta.lore(mmNoItalic(List.of("<gray>Bấm để mời người chơi này.</gray>")));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_INVITE_PLAYER);
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target.getUniqueId().toString());
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildActionItem(Material material, String name, List<String> lore, String action, String target) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic(name));
			meta.lore(mmNoItalic(lore));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
			if (target != null) {
				meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target);
			}
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildInfoItem(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic(name));
			meta.lore(mmNoItalic(lore));
			item.setItemMeta(meta);
		}
		return item;
	}

	private Component mmNoItalic(String miniMessage) {
		return Text.mm(miniMessage).decoration(TextDecoration.ITALIC, false);
	}

	private List<Component> mmNoItalic(List<String> lines) {
		return lines.stream()
			.map(this::mmNoItalic)
			.toList();
	}

	public enum GuiType {
		MAIN,
		LOBBY,
		INVITE,
		SETTINGS,
		SPEEDRUNNER,
		KIT_EDITOR,
		KIT_CLEAR_CONFIRM,
		CARDS
	}

	public record GuiHolder(GuiType type, String matchId) implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return null;
		}
	}

	private ItemStack buildSpeedrunnerItem(Player target) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SkullMeta skullMeta) {
			skullMeta.setOwningPlayer(target);
			skullMeta.displayName(mmNoItalic("<yellow>" + target.getName() + "</yellow>"));
			skullMeta.lore(mmNoItalic(List.of("<gray>Bấm để chọn người chơi này.</gray>")));
			skullMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_SPEEDRUNNER_PICK);
			skullMeta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
				target.getUniqueId().toString());
			item.setItemMeta(skullMeta);
		} else if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>" + target.getName() + "</yellow>"));
			meta.lore(mmNoItalic(List.of("<gray>Bấm để chọn người chơi này.</gray>")));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_SPEEDRUNNER_PICK);
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING,
				target.getUniqueId().toString());
			item.setItemMeta(meta);
		}
		return item;
	}

}
