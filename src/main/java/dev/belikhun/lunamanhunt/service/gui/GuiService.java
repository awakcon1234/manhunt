package dev.belikhun.lunamanhunt.service.gui;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.model.match.WorldDifficultySetting;
import dev.belikhun.lunamanhunt.model.match.WorldTimeSetting;
import dev.belikhun.lunamanhunt.model.match.WorldWeatherSetting;
import dev.belikhun.lunamanhunt.model.kit.SpeedrunnerKitDefinition;
import dev.belikhun.lunamanhunt.model.card.CardDefinition;
import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.profile.MatchHistoryEntry;
import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import dev.belikhun.lunamanhunt.service.card.CardService;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyActionResult;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.service.point.PointService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
	private static final String ACTION_MATCH_INFO_INVITE = "match_info_invite";
	private static final String ACTION_MATCH_INFO_SETTINGS = "match_info_settings";
	private static final String ACTION_MATCH_INFO_CARDS = "match_info_cards";
	private static final String ACTION_MATCH_INFO_KIT = "match_info_kit";
	private static final String ACTION_MATCH_INFO_START = "match_info_start";
	private static final String ACTION_MATCH_INFO_LEAVE = "match_info_leave";
	private static final String ACTION_MATCH_INFO_CLOSE = "match_info_close";
	private static final String ACTION_MATCH_INFO_PLAYERS = "match_info_players";
	private static final String ACTION_MATCH_PLAYERS_BACK = "match_players_back";
	private static final String ACTION_MATCH_PLAYERS_PREV = "match_players_prev";
	private static final String ACTION_MATCH_PLAYERS_NEXT = "match_players_next";
	private static final String ACTION_MATCH_PLAYER_PROFILE = "match_player_profile";

	private static final String ACTION_INVITE_PLAYER = "invite_player";
	private static final String ACTION_INVITE_BACK = "invite_back";

	private static final String ACTION_SETTINGS_VISIBILITY_PUBLIC = "settings_visibility_public";
	private static final String ACTION_SETTINGS_VISIBILITY_INVITE = "settings_visibility_invite";
	private static final String ACTION_SETTINGS_VISIBILITY_PRIVATE = "settings_visibility_private";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_RANDOM = "settings_speedrunner_random";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_PICK = "settings_speedrunner_pick";
	private static final String ACTION_SETTINGS_SPEEDRUNNER_ME = "settings_speedrunner_me";
	private static final String ACTION_SETTINGS_RESET_VISIBILITY = "settings_reset_visibility";
	private static final String ACTION_SETTINGS_RESET_SPEEDRUNNER = "settings_reset_speedrunner";
	private static final String ACTION_SETTINGS_RESET_SEED = "settings_reset_seed";
	private static final String ACTION_SETTINGS_RESET_GEN_VERSION = "settings_reset_gen_version";
	private static final String ACTION_SETTINGS_RESET_WORLD_TIME = "settings_reset_world_time";
	private static final String ACTION_SETTINGS_RESET_WORLD_WEATHER = "settings_reset_world_weather";
	private static final String ACTION_SETTINGS_RESET_WORLD_DIFFICULTY = "settings_reset_world_difficulty";
	private static final String ACTION_SETTINGS_RESET_KEEP_INVENTORY = "settings_reset_keep_inventory";
	private static final String ACTION_SETTINGS_RESET_KIT = "settings_reset_kit";
	private static final String ACTION_SETTINGS_RESET_BORDER = "settings_reset_border";
	private static final String ACTION_SETTINGS_RESET_START_BORDER = "settings_reset_start_border";
	private static final String ACTION_SETTINGS_RESET_CARDS = "settings_reset_cards";
	private static final String ACTION_SETTINGS_SEED = "settings_seed";
	private static final String ACTION_SETTINGS_SEED_CLEAR = "settings_seed_clear";
	private static final String ACTION_SETTINGS_GEN_VERSION_SET = "settings_gen_version_set";
	private static final String ACTION_SETTINGS_GEN_VERSION_INPUT = "settings_gen_version_input";
	private static final String ACTION_SETTINGS_WORLD_TIME_SET = "settings_world_time_set";
	private static final String ACTION_SETTINGS_WORLD_WEATHER_SET = "settings_world_weather_set";
	private static final String ACTION_SETTINGS_WORLD_DIFFICULTY_SET = "settings_world_difficulty_set";
	private static final String ACTION_SETTINGS_KEEP_INVENTORY_SET = "settings_keep_inventory_set";
	private static final String ACTION_SETTINGS_KIT_SELECT = "settings_kit_select";
	private static final String ACTION_SETTINGS_KIT_PAGE_PREV = "settings_kit_page_prev";
	private static final String ACTION_SETTINGS_KIT_PAGE_NEXT = "settings_kit_page_next";
	private static final String ACTION_SETTINGS_CARDS = "settings_cards";
	private static final String ACTION_SETTINGS_BORDER_CHANGE = "settings_border_change";
	private static final String ACTION_SETTINGS_START_BORDER_CHANGE = "settings_start_border_change";
	private static final String ACTION_SETTINGS_PAGE_PREV = "settings_page_prev";
	private static final String ACTION_SETTINGS_PAGE_NEXT = "settings_page_next";
	private static final String ACTION_SETTINGS_BACK = "settings_back";

	private static final String ACTION_SPEEDRUNNER_RANDOM = "speedrunner_random";
	private static final String ACTION_SPEEDRUNNER_PICK = "speedrunner_pick";
	private static final String ACTION_SPEEDRUNNER_BACK = "speedrunner_back";
	private static final String ACTION_KIT_PREVIEW_SELECT = "kit_preview_select";
	private static final String ACTION_KIT_PREVIEW_BACK = "kit_preview_back";

	private static final String ACTION_CARD_USE = "card_use";
	private static final String ACTION_CARD_KEEP = "card_keep";
	private static final String ACTION_CARD_DISCARD = "card_discard";
	private static final String ACTION_CARD_CLOSE = "card_close";
	private static final String ACTION_CARDS_INFO_CLOSE = "cards_info_close";
	private static final String ACTION_CARDS_INFO_PREV = "cards_info_prev";
	private static final String ACTION_CARDS_INFO_NEXT = "cards_info_next";
	private static final String ACTION_CARDS_ADJUST_ENABLE = "cards_adjust_enable";
	private static final String ACTION_CARDS_ADJUST_DISABLE = "cards_adjust_disable";
	private static final String ACTION_CARDS_ADJUST_CLOSE = "cards_adjust_close";
	private static final String ACTION_CARDS_ADJUST_PREV = "cards_adjust_prev";
	private static final String ACTION_CARDS_ADJUST_NEXT = "cards_adjust_next";
	private static final String ACTION_PROFILE_CLOSE = "profile_close";
	private static final String ACTION_PROFILE_HISTORY_OPEN = "profile_history_open";
	private static final String ACTION_PROFILE_HISTORY_BACK = "profile_history_back";
	private static final String ACTION_PROFILE_HISTORY_PREV = "profile_history_prev";
	private static final String ACTION_PROFILE_HISTORY_NEXT = "profile_history_next";

	private static final int HISTORY_PAGE_SIZE = 45;
	private static final int MATCH_PLAYERS_PAGE_SIZE = 45;
	private static final int CARDS_INFO_PAGE_SIZE = 45;
	private static final int CARDS_ADJUST_PAGE_SIZE = 20;
	private static final int SETTINGS_ROWS_PER_PAGE = 4;
	private static final int SETTINGS_TOTAL_ROWS = 12;
	private static final int SETTINGS_ROW_KIT_INDEX = 10;
	private static final int SETTINGS_KIT_PAGE_SIZE = 3;
	private static final int SETTINGS_KIT_PAGE_SIZE_FULL = 5;

	private static final int[] AVAILABLE_CARD_SLOTS = new int[] {
		0, 1, 2, 3,
		9, 10, 11, 12,
		18, 19, 20, 21,
		27, 28, 29, 30,
		36, 37, 38, 39
	};
	private static final int[] ENABLED_CARD_SLOTS = new int[] {
		5, 6, 7, 8,
		14, 15, 16, 17,
		23, 24, 25, 26,
		32, 33, 34, 35,
		41, 42, 43, 44
	};

	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final CardService cardService;
	private final PointService pointService;
	private final ProfileStore profileStore;
	private final SpeedrunnerKitService kitService;
	private final dev.belikhun.lunamanhunt.service.match.MatchSettingsService matchSettingsService;
	private LobbyItemService lobbyItemService;
	private final NamespacedKey actionKey;
	private final NamespacedKey targetKey;
	private final java.util.Map<UUID, String> seedInputs = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, String> genVersionInputs = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, Integer> settingsPages = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, Integer> kitPages = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, Integer> kitPreviewPages = new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<UUID, UUID> profileViewTargets = new java.util.concurrent.ConcurrentHashMap<>();

	public GuiService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager, CardService cardService,
			PointService pointService, ProfileStore profileStore,
			dev.belikhun.lunamanhunt.service.match.MatchSettingsService matchSettingsService,
			SpeedrunnerKitService kitService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.cardService = Objects.requireNonNull(cardService, "cardService");
		this.pointService = Objects.requireNonNull(pointService, "pointService");
		this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
		this.matchSettingsService = Objects.requireNonNull(matchSettingsService, "matchSettingsService");
		this.kitService = Objects.requireNonNull(kitService, "kitService");
		this.actionKey = new NamespacedKey(plugin, ACTION_KEY);
		this.targetKey = new NamespacedKey(plugin, TARGET_KEY);
	}

	public void setLobbyItemService(LobbyItemService lobbyItemService) {
		this.lobbyItemService = lobbyItemService;
	}

	public void openMainMenu(Player player) {
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.MAIN, null), 54,
			Text.mm(UiColors.titlePrimary("Phòng đấu")));
		fillMainMenu(player, inventory);
		player.openInventory(inventory);
	}

	public void openInviteMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.INVITE, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Mời người chơi")));
		fillInviteMenu(player, match, inventory);
		player.openInventory(inventory);
	}

	public void openSettingsMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	public void openSettingsMenu(Player player, Match match, int page) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		int totalPages = getSettingsPageCount();
		int safePage = clamp(page, 0, Math.max(0, totalPages - 1));
		settingsPages.put(player.getUniqueId(), safePage);
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.SETTINGS, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Cài đặt phòng") + " "
				+ UiColors.titleSecondary("- Trang " + (safePage + 1) + "/" + Math.max(1, totalPages))));
		fillSettingsMenu(player, match, inventory, safePage, totalPages);
		player.openInventory(inventory);
	}

	public void openSpeedrunnerMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.SPEEDRUNNER, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Chọn speedrunner")));
		fillSpeedrunnerMenu(match, inventory);
		player.openInventory(inventory);
	}

	public void openKitPreviewMenu(Player player, Match match, SpeedrunnerKitDefinition kit) {
		if (match == null || kit == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy bộ đồ.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.KIT_PREVIEW, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Bộ đồ speedrunner")));
		fillKitPreviewMenu(match, kit, inventory);
		player.openInventory(inventory);
	}

	public void openCardsMenu(Player player, Match match) {
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.CARDS, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Thẻ bài")));
		fillCardsMenu(match, inventory);
		player.openInventory(inventory);
	}

	public void openMatchInfoMenu(Player player) {
		if (player == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.LOBBY) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong sảnh phòng nào.");
			return;
		}
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.MATCH_INFO, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Thông tin phòng")));
		fillMatchInfoMenu(player, match, inventory);
		player.openInventory(inventory);
	}

	public void openCardsAdjustMenu(Player player, Match match, int page) {
		if (player == null || match == null) {
			return;
		}
		boolean isOwner = match.getOwnerId().equals(player.getUniqueId());
		int totalPages = getCardsAdjustPageCount(match, isOwner);
		int safePage = clamp(page, 0, Math.max(0, totalPages - 1));
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.CARDS_ADJUST, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Thẻ dùng trong trận") + " "
				+ UiColors.titleSecondary("- Trang " + (safePage + 1) + "/" + Math.max(1, totalPages))));
		fillCardsAdjustMenu(player, match, inventory, safePage, totalPages);
		player.openInventory(inventory);
	}

	public void openCardsInfoMenu(Player player, int page) {
		if (player == null) {
			return;
		}
		List<CardDefinition> pool = cardService.getCardPool();
		int totalPages = getCardsInfoPageCount(pool.size());
		int safePage = clamp(page, 0, Math.max(0, totalPages - 1));
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.CARDS_INFO, null), 54,
			Text.mm(UiColors.titlePrimary("Danh sách thẻ") + " "
				+ UiColors.titleSecondary("- Trang " + (safePage + 1) + "/" + Math.max(1, totalPages))));
		fillCardsInfoMenu(pool, inventory, safePage, totalPages);
		player.openInventory(inventory);
	}

	public void openProfileMenu(Player player) {
		if (player == null) {
			return;
		}
		openProfileMenu(player, player.getUniqueId());
	}

	public void openProfileMenu(Player viewer, UUID targetId) {
		if (viewer == null) {
			return;
		}
		UUID resolvedId = targetId == null ? viewer.getUniqueId() : targetId;
		profileViewTargets.put(viewer.getUniqueId(), resolvedId);
		PlayerProfile profile = profileStore.load(resolvedId);
		String targetName = resolvePlayerName(resolvedId);
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.PROFILE, null), 27,
			Text.mm(UiColors.titlePrimary("Hồ sơ") + " " + UiColors.titleSecondary(targetName)));
		fillProfileMenu(viewer, profile, inventory, resolvedId);
		viewer.openInventory(inventory);
	}

	public void openProfileHistoryMenu(Player player, int page) {
		if (player == null) {
			return;
		}
		UUID targetId = resolveProfileTarget(player);
		PlayerProfile profile = profileStore.load(targetId);
		int totalPages = getHistoryPageCount(profile.getMatchHistory().size());
		int safePage = clamp(page, 0, Math.max(0, totalPages - 1));
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.PROFILE_HISTORY, null), 54,
			Text.mm(UiColors.titlePrimary("Lịch sử") + " "
				+ UiColors.titleSecondary("- Trang " + (safePage + 1) + "/" + Math.max(1, totalPages))));
		fillProfileHistoryMenu(profile, inventory, safePage, totalPages);
		player.openInventory(inventory);
	}

	public void openMatchPlayersMenu(Player player, Match match, int page) {
		if (player == null || match == null) {
			return;
		}
		List<UUID> players = getSortedMatchPlayers(match);
		int totalPages = getMatchPlayersPageCount(players.size());
		int safePage = clamp(page, 0, Math.max(0, totalPages - 1));
		Inventory inventory = Bukkit.createInventory(new GuiHolder(GuiType.MATCH_PLAYERS, match.getId()), 54,
			Text.mm(UiColors.titlePrimary("Người chơi trong phòng") + " "
				+ UiColors.titleSecondary("- Trang " + (safePage + 1) + "/" + Math.max(1, totalPages))));
		fillMatchPlayersMenu(match, players, inventory, safePage, totalPages);
		player.openInventory(inventory);
	}

	public boolean isGuiInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof GuiHolder;
	}

	public boolean handleMenuClick(Player player, Inventory inventory, ItemStack item, ClickType clickType) {
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

		player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.0f);

		switch (action) {
			case ACTION_MAIN_CREATE -> handleCreate(player);
			case ACTION_MAIN_MY -> handleMyMatch(player);
			case ACTION_MAIN_SETTINGS -> Text.msg(player, "<yellow>ℹ</yellow> Cài đặt đang được phát triển.");
			case ACTION_MAIN_CARDS -> handleOpenCards(player);
			case ACTION_MAIN_REFRESH -> openMainMenu(player);
			case ACTION_MAIN_CLOSE -> player.closeInventory();
			case ACTION_MATCH_JOIN -> handleJoin(player, target);
			case ACTION_MATCH_INFO_INVITE -> handleMatchInfoInvite(player, holder.matchId());
			case ACTION_MATCH_INFO_SETTINGS -> handleMatchInfoSettings(player, holder.matchId());
			case ACTION_MATCH_INFO_CARDS -> handleMatchInfoCards(player, holder.matchId(), target);
			case ACTION_MATCH_INFO_KIT -> handleMatchInfoKit(player, holder.matchId());
			case ACTION_MATCH_INFO_START -> handleMatchInfoStart(player, holder.matchId());
			case ACTION_MATCH_INFO_PLAYERS -> handleMatchInfoPlayers(player, holder.matchId(), target);
			case ACTION_MATCH_INFO_LEAVE -> handleMatchInfoLeave(player);
			case ACTION_MATCH_INFO_CLOSE -> player.closeInventory();
			case ACTION_MATCH_PLAYERS_BACK -> handleMatchPlayersBack(player, holder.matchId());
			case ACTION_MATCH_PLAYERS_PREV, ACTION_MATCH_PLAYERS_NEXT -> handleMatchPlayersPage(player, holder.matchId(), target);
			case ACTION_MATCH_PLAYER_PROFILE -> handleOpenMatchPlayerProfile(player, target);
			case ACTION_INVITE_PLAYER -> handleInvitePlayer(player, holder.matchId(), target);
			case ACTION_INVITE_BACK -> handleBackToLobby(player, holder.matchId());
			case ACTION_SETTINGS_VISIBILITY_PUBLIC -> handleVisibilitySet(player, holder.matchId(), MatchVisibility.PUBLIC);
			case ACTION_SETTINGS_VISIBILITY_INVITE -> handleVisibilitySet(player, holder.matchId(), MatchVisibility.INVITE_ONLY);
			case ACTION_SETTINGS_VISIBILITY_PRIVATE -> handleVisibilitySet(player, holder.matchId(), MatchVisibility.PRIVATE);
			case ACTION_SETTINGS_SPEEDRUNNER_RANDOM -> handleSpeedrunnerRandom(player, holder.matchId());
			case ACTION_SETTINGS_SPEEDRUNNER_PICK -> handleOpenSpeedrunnerMenu(player, holder.matchId());
			case ACTION_SETTINGS_SPEEDRUNNER_ME -> handleSpeedrunnerMe(player, holder.matchId());
			case ACTION_SETTINGS_RESET_VISIBILITY -> handleResetVisibility(player, holder.matchId());
			case ACTION_SETTINGS_RESET_SPEEDRUNNER -> handleResetSpeedrunner(player, holder.matchId());
			case ACTION_SETTINGS_RESET_SEED -> handleResetSeed(player, holder.matchId());
			case ACTION_SETTINGS_RESET_GEN_VERSION -> handleResetWorldGenerationVersion(player, holder.matchId());
			case ACTION_SETTINGS_RESET_WORLD_TIME -> handleResetWorldTime(player, holder.matchId());
			case ACTION_SETTINGS_RESET_WORLD_WEATHER -> handleResetWorldWeather(player, holder.matchId());
			case ACTION_SETTINGS_RESET_WORLD_DIFFICULTY -> handleResetWorldDifficulty(player, holder.matchId());
			case ACTION_SETTINGS_RESET_KEEP_INVENTORY -> handleResetKeepInventory(player, holder.matchId());
			case ACTION_SETTINGS_RESET_KIT -> handleResetKit(player, holder.matchId());
			case ACTION_SETTINGS_RESET_BORDER -> handleResetBorder(player, holder.matchId());
			case ACTION_SETTINGS_RESET_START_BORDER -> handleResetStartBorder(player, holder.matchId());
			case ACTION_SETTINGS_RESET_CARDS -> handleResetCards(player, holder.matchId());
			case ACTION_SETTINGS_SEED -> handleSeedPrompt(player, holder.matchId());
			case ACTION_SETTINGS_SEED_CLEAR -> handleSeedClear(player, holder.matchId());
			case ACTION_SETTINGS_GEN_VERSION_SET -> handleWorldGenerationVersionSet(player, holder.matchId(), target);
			case ACTION_SETTINGS_GEN_VERSION_INPUT -> handleWorldGenerationVersionPrompt(player, holder.matchId());
			case ACTION_SETTINGS_WORLD_TIME_SET -> handleWorldTimeSet(player, holder.matchId(), target);
			case ACTION_SETTINGS_WORLD_WEATHER_SET -> handleWorldWeatherSet(player, holder.matchId(), target);
			case ACTION_SETTINGS_WORLD_DIFFICULTY_SET -> handleWorldDifficultySet(player, holder.matchId(), target);
			case ACTION_SETTINGS_KEEP_INVENTORY_SET -> handleKeepInventorySet(player, holder.matchId(), target);
			case ACTION_SETTINGS_KIT_SELECT -> handleOpenKitPreview(player, holder.matchId(), target);
			case ACTION_SETTINGS_KIT_PAGE_PREV, ACTION_SETTINGS_KIT_PAGE_NEXT -> handleOpenKitPage(player, holder.matchId(), target);
			case ACTION_SETTINGS_CARDS -> handleMatchInfoCards(player, holder.matchId(), "0");
			case ACTION_SETTINGS_BORDER_CHANGE -> handleBorderChange(player, holder.matchId(), parseDelta(target, 0));
			case ACTION_SETTINGS_START_BORDER_CHANGE -> handleStartBorderChange(player, holder.matchId(), parseDelta(target, 0));
			case ACTION_SETTINGS_PAGE_PREV, ACTION_SETTINGS_PAGE_NEXT -> handleOpenSettingsPage(player, holder.matchId(), target);
			case ACTION_SETTINGS_BACK -> handleBackToLobby(player, holder.matchId());
			case ACTION_SPEEDRUNNER_RANDOM -> handleSpeedrunnerRandom(player, holder.matchId());
			case ACTION_SPEEDRUNNER_PICK -> handleSpeedrunnerPick(player, holder.matchId(), target);
			case ACTION_SPEEDRUNNER_BACK -> handleOpenSettings(player, holder.matchId());
			case ACTION_KIT_PREVIEW_SELECT -> handleSelectKit(player, holder.matchId(), target);
			case ACTION_KIT_PREVIEW_BACK -> handleBackFromKitPreview(player, holder.matchId());
			case ACTION_CARD_USE -> handleUseCard(player, holder.matchId(), target);
			case ACTION_CARD_KEEP -> {
				if (clickType == ClickType.RIGHT || clickType == ClickType.SHIFT_RIGHT) {
					handleKeepCard(player, holder.matchId(), target);
				} else {
					handleUsePoolCard(player, holder.matchId(), target);
				}
			}
			case ACTION_CARD_DISCARD -> handleDiscardCard(player, holder.matchId(), target);
			case ACTION_CARD_CLOSE -> player.closeInventory();
			case ACTION_CARDS_INFO_CLOSE -> player.closeInventory();
			case ACTION_CARDS_INFO_PREV, ACTION_CARDS_INFO_NEXT -> handleOpenCardsInfoPage(player, target);
			case ACTION_CARDS_ADJUST_ENABLE -> handleAdjustCard(player, holder.matchId(), target, true);
			case ACTION_CARDS_ADJUST_DISABLE -> handleAdjustCard(player, holder.matchId(), target, false);
			case ACTION_CARDS_ADJUST_PREV, ACTION_CARDS_ADJUST_NEXT -> handleOpenCardsAdjustPage(player, holder.matchId(), target);
			case ACTION_CARDS_ADJUST_CLOSE -> handleCardsAdjustClose(player, holder.matchId());
			case ACTION_PROFILE_CLOSE -> player.closeInventory();
			case ACTION_PROFILE_HISTORY_OPEN -> openProfileHistoryMenu(player, 0);
			case ACTION_PROFILE_HISTORY_BACK -> openProfileMenu(player, resolveProfileTarget(player));
			case ACTION_PROFILE_HISTORY_PREV, ACTION_PROFILE_HISTORY_NEXT -> handleOpenHistoryPage(player, target);
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

		inventory.setItem(49, buildActionItem(Material.CLOCK,
			"<yellow>ℹ</yellow> Làm mới",
			List.of("<gray>Cập nhật danh sách phòng.</gray>"), ACTION_MAIN_REFRESH, null));

		inventory.setItem(53, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng bảng chọn.</gray>"), ACTION_MAIN_CLOSE, null));
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
			List.of("<gray>Cập nhật danh sách.</gray>"), ACTION_MATCH_INFO_INVITE, null));

		inventory.setItem(53, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về phòng.</gray>"), ACTION_INVITE_BACK, null));
	}

	private void fillSettingsMenu(Player player, Match match, Inventory inventory, int page, int totalPages) {
		MatchSettings settings = match.getSettings();
		MatchSettings defaults = matchSettingsService.createDefaults();
		String seedText = formatSeedValue(settings.getWorldSeed(), defaults.getWorldSeed());
		String defaultSeedText = formatSeedValue(defaults.getWorldSeed(), defaults.getWorldSeed());
		String genVersionText = formatGenVersionValue(
			settings.getWorldGenerationVersion(), defaults.getWorldGenerationVersion());
		String defaultGenVersionText = formatGenVersionValue(
			defaults.getWorldGenerationVersion(), defaults.getWorldGenerationVersion());
		String worldTimeText = formatWorldTimeColored(settings.getWorldTime(), defaults.getWorldTime());
		String defaultWorldTimeText = formatWorldTimeColored(defaults.getWorldTime(), defaults.getWorldTime());
		String worldWeatherText = formatWorldWeatherColored(settings.getWorldWeather(), defaults.getWorldWeather());
		String defaultWorldWeatherText = formatWorldWeatherColored(
			defaults.getWorldWeather(), defaults.getWorldWeather());
		String worldDifficultyText = formatWorldDifficultyColored(
			settings.getWorldDifficulty(), defaults.getWorldDifficulty());
		String defaultWorldDifficultyText = formatWorldDifficultyColored(
			defaults.getWorldDifficulty(), defaults.getWorldDifficulty());
		String keepInventoryText = formatKeepInventoryColored(settings.isKeepInventory(), defaults.isKeepInventory());
		String defaultKeepInventoryText = formatKeepInventoryColored(
			defaults.isKeepInventory(), defaults.isKeepInventory());
		String defaultKitId = resolveDefaultKitId(defaults);
		String kitText = formatKitNameColored(settings, defaultKitId);
		String defaultKitText = formatKitNameColored(defaults, defaultKitId);
		int kitCount = kitService.resolveKitItems(settings).size();
		int defaultKitCount = kitService.resolveKitItems(defaults).size();
		int enabledCards = cardService.getEnabledCards(match).size();
		int defaultEnabledCards = defaults.getEnabledCardIds().size();

		ItemStack pane = buildInfoItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(" "));
		int[] frameSlots = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8,
			9, 17, 18, 26, 27, 35, 36, 44,
			45, 46, 47, 48, 49, 50, 51, 52, 53
		};
		for (int slot : frameSlots) {
			inventory.setItem(slot, pane);
		}

		List<SettingRow> rows = new ArrayList<>();
		rows.add(new SettingRow(
			Material.NAME_TAG,
			"<yellow>ℹ</yellow> Hiển thị",
			List.of(
				"<gray>Hiện tại: " + formatVisibilityColored(settings.getVisibility()) + "</gray>",
				"<gray>Mặc định: " + formatVisibilityColored(defaults.getVisibility()) + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_VISIBILITY,
			List.of(
				buildOption(Material.LIME_WOOL, "<green>✔</green> Công khai",
					List.of("<gray>Cho mọi người tham gia.</gray>"), ACTION_SETTINGS_VISIBILITY_PUBLIC, null),
				buildOption(Material.YELLOW_WOOL, "<yellow>ℹ</yellow> Chỉ mời",
					List.of("<gray>Chỉ người được mời mới vào.</gray>"), ACTION_SETTINGS_VISIBILITY_INVITE, null),
				buildOption(Material.RED_WOOL, "<red>❌</red> Riêng tư",
					List.of("<gray>Ẩn khỏi danh sách phòng.</gray>"), ACTION_SETTINGS_VISIBILITY_PRIVATE, null)
			)
		));

		rows.add(new SettingRow(
			Material.COMPASS,
			"<yellow>ℹ</yellow> Speedrunner",
			List.of(
				"<gray>Hiện tại: " + formatSpeedrunnerColored(settings) + "</gray>",
				"<gray>Mặc định: " + formatSpeedrunnerColored(defaults) + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_SPEEDRUNNER,
			List.of(
				buildOption(Material.COMPASS, "<yellow>ℹ</yellow> Ngẫu nhiên",
					List.of("<gray>Tự chọn speedrunner.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_RANDOM, null),
				buildOption(Material.PLAYER_HEAD, "<yellow>ℹ</yellow> Chọn người chơi",
					List.of("<gray>Chọn từ danh sách.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_PICK, null),
				buildOption(Material.PLAYER_HEAD, "<yellow>ℹ</yellow> Là chính bạn",
					List.of("<gray>Đặt bạn làm speedrunner.</gray>"), ACTION_SETTINGS_SPEEDRUNNER_ME, null)
			)
		));

		rows.add(new SettingRow(
			Material.IRON_BARS,
			"<yellow>ℹ</yellow> Viền tối đa",
			List.of(
				"<gray>Hiện tại: " + formatNumericValue(settings.getWorldBorderLimit(), defaults.getWorldBorderLimit()) + "</gray>",
				"<gray>Mặc định: " + formatNumericValue(defaults.getWorldBorderLimit(), defaults.getWorldBorderLimit()) + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_BORDER,
			List.of(
				buildOption(Material.RED_WOOL, "<red>❌</red> -500",
					List.of("<gray>Giảm nhanh 500 block.</gray>"), ACTION_SETTINGS_BORDER_CHANGE, "-500"),
				buildOption(Material.RED_WOOL, "<red>❌</red> -100",
					List.of("<gray>Giảm 100 block.</gray>"), ACTION_SETTINGS_BORDER_CHANGE, "-100"),
				buildOption(Material.LIME_WOOL, "<green>✔</green> +100",
					List.of("<gray>Tăng 100 block.</gray>"), ACTION_SETTINGS_BORDER_CHANGE, "100"),
				buildOption(Material.LIME_WOOL, "<green>✔</green> +500",
					List.of("<gray>Tăng nhanh 500 block.</gray>"), ACTION_SETTINGS_BORDER_CHANGE, "500")
			)
		));

		rows.add(new SettingRow(
			Material.SCAFFOLDING,
			"<yellow>ℹ</yellow> Viền ban đầu",
			List.of(
				"<gray>Hiện tại: " + formatNumericValue(settings.getStartingBorder(), defaults.getStartingBorder()) + "</gray>",
				"<gray>Mặc định: " + formatNumericValue(defaults.getStartingBorder(), defaults.getStartingBorder()) + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_START_BORDER,
			List.of(
				buildOption(Material.RED_WOOL, "<red>❌</red> -5",
					List.of("<gray>Giảm nhanh 5 block.</gray>"), ACTION_SETTINGS_START_BORDER_CHANGE, "-5"),
				buildOption(Material.RED_WOOL, "<red>❌</red> -1",
					List.of("<gray>Giảm 1 block.</gray>"), ACTION_SETTINGS_START_BORDER_CHANGE, "-1"),
				buildOption(Material.LIME_WOOL, "<green>✔</green> +1",
					List.of("<gray>Tăng 1 block.</gray>"), ACTION_SETTINGS_START_BORDER_CHANGE, "1"),
				buildOption(Material.LIME_WOOL, "<green>✔</green> +5",
					List.of("<gray>Tăng nhanh 5 block.</gray>"), ACTION_SETTINGS_START_BORDER_CHANGE, "5")
			)
		));

		rows.add(new SettingRow(
			Material.PAPER,
			"<yellow>ℹ</yellow> Seed",
			List.of(
				"<gray>Hiện tại: " + seedText + "</gray>",
				"<gray>Mặc định: " + defaultSeedText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_SEED,
			List.of(
				buildOption(Material.NAME_TAG, "<yellow>ℹ</yellow> Nhập seed",
					List.of("<gray>Nhập bằng chat.</gray>"), ACTION_SETTINGS_SEED, null),
				buildOption(Material.BARRIER, "<red>❌</red> Xóa seed",
					List.of("<gray>Đặt seed về trống.</gray>"), ACTION_SETTINGS_SEED_CLEAR, null)
			)
		));

		rows.add(new SettingRow(
			Material.REPEATER,
			"<yellow>ℹ</yellow> Phiên bản tạo thế giới",
			List.of(
				"<gray>Hiện tại: " + genVersionText + "</gray>",
				"<gray>Mặc định: " + defaultGenVersionText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_GEN_VERSION,
			List.of(
				buildOption(Material.COMPARATOR, "<yellow>ℹ</yellow> Theo server",
					List.of("<gray>Dùng phiên bản máy chủ.</gray>"), ACTION_SETTINGS_GEN_VERSION_SET, "current"),
				buildOption(Material.DIAMOND, "<yellow>ℹ</yellow> 1.21.11",
					List.of("<gray>Phiên bản mới nhất.</gray>"), ACTION_SETTINGS_GEN_VERSION_SET, "1.21.11"),
				buildOption(Material.CRYING_OBSIDIAN, "<yellow>ℹ</yellow> 1.16.1",
					List.of("<gray>Phiên bản Nether.</gray>"), ACTION_SETTINGS_GEN_VERSION_SET, "1.16.1"),
				buildOption(Material.GOLD_INGOT, "<yellow>ℹ</yellow> 1.12",
					List.of("<gray>Phiên bản cũ.</gray>"), ACTION_SETTINGS_GEN_VERSION_SET, "1.12"),
				buildOption(Material.NAME_TAG, "<yellow>ℹ</yellow> Nhập phiên bản",
					List.of("<gray>Gõ phiên bản bất kỳ.</gray>"), ACTION_SETTINGS_GEN_VERSION_INPUT, null)
			)
		));

		rows.add(new SettingRow(
			Material.CLOCK,
			"<yellow>ℹ</yellow> Thời gian thế giới",
			List.of(
				"<gray>Hiện tại: " + worldTimeText + "</gray>",
				"<gray>Mặc định: " + defaultWorldTimeText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_WORLD_TIME,
			List.of(
				buildOption(Material.CLOCK, "<yellow>ℹ</yellow> Chu kỳ",
					List.of("<gray>Thời gian tự chạy.</gray>"), ACTION_SETTINGS_WORLD_TIME_SET,
					WorldTimeSetting.CYCLE.name()),
				buildOption(Material.SUNFLOWER, "<yellow>ℹ</yellow> Buổi sáng",
					List.of("<gray>Giữ buổi sáng.</gray>"), ACTION_SETTINGS_WORLD_TIME_SET,
					WorldTimeSetting.MORNING.name()),
				buildOption(Material.GOLD_BLOCK, "<yellow>ℹ</yellow> Buổi trưa",
					List.of("<gray>Giữ buổi trưa.</gray>"), ACTION_SETTINGS_WORLD_TIME_SET,
					WorldTimeSetting.NOON.name()),
				buildOption(Material.BLUE_BED, "<yellow>ℹ</yellow> Ban đêm",
					List.of("<gray>Giữ ban đêm.</gray>"), ACTION_SETTINGS_WORLD_TIME_SET,
					WorldTimeSetting.NIGHT.name()),
				buildOption(Material.ENDER_PEARL, "<yellow>ℹ</yellow> Nửa đêm",
					List.of("<gray>Giữ nửa đêm.</gray>"), ACTION_SETTINGS_WORLD_TIME_SET,
					WorldTimeSetting.MIDNIGHT.name())
			)
		));

		rows.add(new SettingRow(
			Material.WATER_BUCKET,
			"<yellow>ℹ</yellow> Thời tiết",
			List.of(
				"<gray>Hiện tại: " + worldWeatherText + "</gray>",
				"<gray>Mặc định: " + defaultWorldWeatherText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_WORLD_WEATHER,
			List.of(
				buildOption(Material.CLOCK, "<yellow>ℹ</yellow> Chu kỳ",
					List.of("<gray>Thời tiết tự chạy.</gray>"), ACTION_SETTINGS_WORLD_WEATHER_SET,
					WorldWeatherSetting.CYCLE.name()),
				buildOption(Material.SUNFLOWER, "<yellow>ℹ</yellow> Trời quang",
					List.of("<gray>Không mưa.</gray>"), ACTION_SETTINGS_WORLD_WEATHER_SET,
					WorldWeatherSetting.CLEAR.name()),
				buildOption(Material.WATER_BUCKET, "<yellow>ℹ</yellow> Mưa",
					List.of("<gray>Luôn có mưa.</gray>"), ACTION_SETTINGS_WORLD_WEATHER_SET,
					WorldWeatherSetting.RAIN.name()),
				buildOption(Material.TRIDENT, "<yellow>ℹ</yellow> Giông",
					List.of("<gray>Luôn có sấm sét.</gray>"), ACTION_SETTINGS_WORLD_WEATHER_SET,
					WorldWeatherSetting.THUNDERSTORM.name())
			)
		));

		rows.add(new SettingRow(
			Material.IRON_SWORD,
			"<yellow>ℹ</yellow> Độ khó",
			List.of(
				"<gray>Hiện tại: " + worldDifficultyText + "</gray>",
				"<gray>Mặc định: " + defaultWorldDifficultyText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_WORLD_DIFFICULTY,
			List.of(
				buildOption(Material.WHITE_WOOL, "<green>✔</green> Hòa bình",
					List.of("<gray>Không có quái.</gray>"), ACTION_SETTINGS_WORLD_DIFFICULTY_SET,
					WorldDifficultySetting.PEACEFUL.name()),
				buildOption(Material.LIME_WOOL, "<green>✔</green> Dễ",
					List.of("<gray>Độ khó nhẹ.</gray>"), ACTION_SETTINGS_WORLD_DIFFICULTY_SET,
					WorldDifficultySetting.EASY.name()),
				buildOption(Material.YELLOW_WOOL, "<yellow>ℹ</yellow> Trung bình",
					List.of("<gray>Độ khó chuẩn.</gray>"), ACTION_SETTINGS_WORLD_DIFFICULTY_SET,
					WorldDifficultySetting.MEDIUM.name()),
				buildOption(Material.RED_WOOL, "<red>❌</red> Khó",
					List.of("<gray>Độ khó cao.</gray>"), ACTION_SETTINGS_WORLD_DIFFICULTY_SET,
					WorldDifficultySetting.HARD.name())
			)
		));

		rows.add(new SettingRow(
			Material.CHEST,
			"<yellow>ℹ</yellow> Giữ đồ khi chết",
			List.of(
				"<gray>Hiện tại: " + keepInventoryText + "</gray>",
				"<gray>Mặc định: " + defaultKeepInventoryText + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_KEEP_INVENTORY,
			List.of(
				buildOption(Material.LIME_WOOL, "<green>✔</green> Bật",
					List.of("<gray>Không rơi đồ.</gray>"), ACTION_SETTINGS_KEEP_INVENTORY_SET, "on"),
				buildOption(Material.RED_WOOL, "<red>❌</red> Tắt",
					List.of("<gray>Rơi đồ khi chết.</gray>"), ACTION_SETTINGS_KEEP_INVENTORY_SET, "off")
			)
		));

		List<SpeedrunnerKitDefinition> allKits = kitService.getKits();
		int kitTotal = allKits.size();
		boolean paginateKits = kitTotal > SETTINGS_KIT_PAGE_SIZE_FULL;
		int kitsPerPage = paginateKits ? SETTINGS_KIT_PAGE_SIZE : SETTINGS_KIT_PAGE_SIZE_FULL;
		int kitTotalPages = Math.max(1, (kitTotal + kitsPerPage - 1) / kitsPerPage);
		int kitPage = getKitPage(player);
		int safeKitPage = clamp(kitPage, 0, kitTotalPages - 1);
		setKitPage(player, safeKitPage);
		int kitStart = safeKitPage * kitsPerPage;
		int kitEnd = Math.min(kitTotal, kitStart + kitsPerPage);
		List<SpeedrunnerKitDefinition> kitPageItems = allKits.subList(kitStart, kitEnd);

		List<OptionItem> kitOptions = new ArrayList<>();
		if (paginateKits && safeKitPage > 0) {
			kitOptions.add(buildPageOption(Material.ARROW, "<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_SETTINGS_KIT_PAGE_PREV,
				String.valueOf(safeKitPage - 1)));
		}

		String selectedKitId = settings.getSpeedrunnerKitId();
		for (SpeedrunnerKitDefinition kit : kitPageItems) {
			List<String> lore = new ArrayList<>(kit.getDescription());
			lore.add(" ");
			lore.add("<gray>Số vật phẩm: <white>" + kit.getItems().size() + "</white></gray>");
			if (selectedKitId != null && kit.getId().equalsIgnoreCase(selectedKitId)) {
				lore.add(" ");
				lore.add("<#7fd36b>Đang chọn</#7fd36b>");
			}
			kitOptions.add(buildPreviewOption(kit.getIcon(), "<yellow>ℹ</yellow> " + kit.getName(), lore,
				ACTION_SETTINGS_KIT_SELECT, kit.getId()));
		}

		if (paginateKits && safeKitPage < kitTotalPages - 1) {
			kitOptions.add(buildPageOption(Material.ARROW, "<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang sau.</gray>"), ACTION_SETTINGS_KIT_PAGE_NEXT,
				String.valueOf(safeKitPage + 1)));
		}

		List<String> kitLore = new ArrayList<>();
		kitLore.add("<gray>Hiện tại: " + kitText + "</gray>");
		kitLore.add("<gray>Mặc định: " + defaultKitText + "</gray>");
		kitLore.add(" ");
		kitLore.add("<gray>Vật phẩm: <white>" + kitCount + "</white></gray>");
		kitLore.add("<gray>Mặc định: <white>" + defaultKitCount + "</white></gray>");
		if (paginateKits) {
			kitLore.add("<gray>Trang: <white>" + (safeKitPage + 1) + "/" + kitTotalPages + "</white></gray>");
		}
		kitLore.add(" ");
		kitLore.add("<gray>Bấm để về mặc định.</gray>");

		rows.add(new SettingRow(
			Material.CHEST,
			"<yellow>ℹ</yellow> Bộ đồ speedrunner",
			kitLore,
			ACTION_SETTINGS_RESET_KIT,
			kitOptions
		));

		rows.add(new SettingRow(
			Material.EMERALD,
			"<yellow>ℹ</yellow> Thẻ dùng trong trận",
			List.of(
				"<gray>Hiện tại: " + formatCountValue(enabledCards, defaultEnabledCards) + "</gray>",
				"<gray>Mặc định: " + formatCountValue(defaultEnabledCards, defaultEnabledCards) + "</gray>",
				" ",
				"<gray>Bấm để về mặc định.</gray>"
			),
			ACTION_SETTINGS_RESET_CARDS,
			List.of(
				buildOption(Material.EMERALD, "<yellow>ℹ</yellow> Chỉnh thẻ",
					List.of("<gray>Bật/tắt thẻ cho trận.</gray>"), ACTION_SETTINGS_CARDS, "0")
			)
		));

		int start = page * SETTINGS_ROWS_PER_PAGE;
		int end = Math.min(rows.size(), start + SETTINGS_ROWS_PER_PAGE);
		ItemStack spacer = buildInfoItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(" "));
		int[] rowBases = new int[] { 10, 19, 28, 37 };
		for (int index = start; index < end; index++) {
			SettingRow row = rows.get(index);
			int rowIndex = index - start;
			if (rowIndex >= rowBases.length) {
				break;
			}
			int base = rowBases[rowIndex];
			inventory.setItem(base, buildActionItem(row.icon(), row.title(), row.lore(), row.resetAction(), null));
			inventory.setItem(base + 1, spacer);
			List<OptionItem> options = row.options();
			for (int i = 0; i < options.size() && i < 5; i++) {
				OptionItem option = options.get(i);
				inventory.setItem(base + 2 + i, buildActionItem(option.icon(), option.title(), option.lore(),
					option.action(), option.target()));
			}
		}

		inventory.setItem(49, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về phòng.</gray>"), ACTION_SETTINGS_BACK, null));
		inventory.setItem(48, buildInfoItem(Material.MAP,
			"<yellow>ℹ</yellow> Trang",
			List.of(
				"<gray>Trang hiện tại: <white>" + (page + 1) + "/" + Math.max(1, totalPages) + "</white></gray>"
			)));
		if (page > 0) {
			inventory.setItem(45, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_SETTINGS_PAGE_PREV, String.valueOf(page - 1)));
		}
		if (page < totalPages - 1) {
			inventory.setItem(53, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang tiếp theo.</gray>"), ACTION_SETTINGS_PAGE_NEXT, String.valueOf(page + 1)));
		}
	}

	private void fillCardsMenu(Match match, Inventory inventory) {
		int points = match.getSpeedrunnerPoints();
		List<CardDefinition> poolCards = cardService.getDrawnCards(match);
		List<CardDefinition> keptCards = cardService.getKeptCards(match);
		ItemStack pane = buildInfoItem(Material.BLACK_STAINED_GLASS_PANE, " ", List.of(" "));
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			inventory.setItem(slot, pane);
		}

		int[] keptSlots = new int[] { 38, 40, 42 };
		for (int slot : keptSlots) {
			inventory.setItem(slot, null);
		}

		inventory.setItem(8, buildInfoItem(Material.BOOK,
			"<yellow>ℹ</yellow> Hướng dẫn",
			List.of(
				"",
				"<gray>● Hàng 3: bấm trái để dùng ngay.</gray>",
				"<gray>● Hàng 3: bấm phải để giữ vào ô thẻ.</gray>",
				"<gray>● Hàng 5: thẻ đang giữ, bấm trái để dùng.</gray>",
				"<gray>● Hàng 6: bấm nút bỏ thẻ nếu ô đã đầy.</gray>"
			)));

		int[] poolSlots = new int[] { 19, 20, 21, 22, 23, 24, 25 };
		for (int i = 0; i < poolSlots.length; i++) {
			if (i >= poolCards.size()) {
				break;
			}
			CardDefinition card = poolCards.get(i);
			if (card == null) {
				continue;
			}
			inventory.setItem(poolSlots[i],
				cardService.buildCardItem(card, i, actionKey, targetKey, ACTION_CARD_KEEP));
		}
		for (int i = 0; i < keptSlots.length; i++) {
			if (i >= keptCards.size()) {
				break;
			}
			CardDefinition card = keptCards.get(i);
			if (card == null) {
				continue;
			}
			inventory.setItem(keptSlots[i],
				cardService.buildCardItem(card, i, actionKey, targetKey, ACTION_CARD_USE));
		}

		int[] discardSlots = new int[] { 47, 49, 51 };
		for (int i = 0; i < discardSlots.length; i++) {
			String label = "<red>❌</red> Bỏ thẻ ô " + (i + 1);
			inventory.setItem(discardSlots[i], buildActionItem(Material.BARRIER, label,
				List.of("<gray>Loại bỏ thẻ đang giữ ở ô này.</gray>"),
				ACTION_CARD_DISCARD, String.valueOf(i)));
		}

		int remainingCards = 0;
		for (CardDefinition card : poolCards) {
			if (card != null) {
				remainingCards++;
			}
		}
		ItemStack pointsItem = new ItemStack(Material.SUNFLOWER);
		ItemMeta pointsMeta = pointsItem.getItemMeta();
		if (pointsMeta != null) {
			pointsMeta.displayName(mmNoItalic("<yellow>ℹ</yellow> Điểm hiện tại"));
			pointsMeta.lore(mmNoItalic(List.of(
				"<gray>Bạn có <white>" + pointService.formatPoints(points) + "</white>.</gray>",
				" ",
				"<gray>Thẻ mới: <white>" + remainingCards + "</white>/" + cardService.getPoolCount() + "</gray>"
			)));
			pointsMeta.setEnchantmentGlintOverride(true);
			pointsItem.setItemMeta(pointsMeta);
		}
		pointsItem.setAmount(Math.max(1, Math.min(64, points)));
		inventory.setItem(44, pointsItem);

		if (remainingCards == 0) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Chưa có thẻ mới",
				List.of("<gray>Nhận thành tựu để mở 7 thẻ.</gray>")));
		}

		inventory.setItem(53, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng bảng thẻ bài.</gray>"), ACTION_CARD_CLOSE, null));
	}

	private void fillKitPreviewMenu(Match match, SpeedrunnerKitDefinition kit, Inventory inventory) {
		ItemStack pane = buildInfoItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(" "));
		int[] frameSlots = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8,
			9, 17, 18, 26, 27, 35, 36, 44,
			45, 46, 47, 48, 49, 50, 51, 52, 53
		};
		for (int slot : frameSlots) {
			inventory.setItem(slot, pane);
		}

		List<String> infoLore = new ArrayList<>(kit.getDescription());
		infoLore.add(" ");
		infoLore.add("<gray>Số vật phẩm: <white>" + kit.getItems().size() + "</white></gray>");
		inventory.setItem(4, buildInfoItem(kit.getIcon(),
			"<yellow>ℹ</yellow> " + kit.getName(), infoLore));

		int[] previewSlots = new int[] {
			10, 11, 12, 13, 14, 15, 16,
			19, 20, 21, 22, 23, 24, 25,
			28, 29, 30, 31, 32, 33, 34,
			37, 38, 39, 40, 41, 42, 43
		};
		List<ItemStack> items = kit.getItems();
		for (int i = 0; i < previewSlots.length && i < items.size(); i++) {
			ItemStack item = items.get(i);
			if (item != null) {
				inventory.setItem(previewSlots[i], item.clone());
			}
		}

		inventory.setItem(45, buildActionItem(Material.ARROW,
			"<yellow>ℹ</yellow> Quay lại",
			List.of("<gray>Quay về cài đặt.</gray>"), ACTION_KIT_PREVIEW_BACK, null));
		inventory.setItem(53, buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Chọn bộ đồ",
			List.of("<gray>Đặt bộ đồ này cho trận.</gray>"), ACTION_KIT_PREVIEW_SELECT, kit.getId()));
	}

	private void fillMatchInfoMenu(Player player, Match match, Inventory inventory) {
		String ownerName = "Không rõ";
		Player owner = Bukkit.getPlayer(match.getOwnerId());
		if (owner != null) {
			ownerName = owner.getName();
		}
		MatchSettings settings = match.getSettings();
		MatchSettings defaults = matchSettingsService.createDefaults();
		String seedText = formatSeedValue(settings.getWorldSeed(), defaults.getWorldSeed());
		String defaultKitId = resolveDefaultKitId(defaults);
		String kitText = formatKitNameColored(settings, defaultKitId);
		String defaultKitText = formatKitNameColored(defaults, defaultKitId);
		int enabledCards = cardService.getEnabledCards(match).size();
		int defaultEnabledCards = defaults.getEnabledCardIds().size();
		boolean isOwner = match.getOwnerId().equals(player.getUniqueId());

		ItemStack pane = buildInfoItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of(" "));
		int[] frameSlots = new int[] {
			0, 1, 2, 3, 4, 5, 6, 7, 8,
			9, 17, 18, 26, 27, 35, 36, 44,
			45, 46, 47, 48, 49, 50, 51, 52, 53
		};
		for (int slot : frameSlots) {
			inventory.setItem(slot, pane);
		}

		inventory.setItem(4, buildInfoItem(Material.BOOK,
			"<yellow>ℹ</yellow> Thông tin phòng",
			List.of(
				"<gray>Mã phòng: <white>" + match.getId() + "</white></gray>",
				"<gray>Chủ phòng: <#7fd36b>" + ownerName + "</#7fd36b></gray>",
				" ",
				"<gray>Hiển thị: " + formatVisibilityColored(match.getSettings().getVisibility()) + "</gray>",
				"<gray>Trạng thái: " + formatStateColored(match.getState()) + "</gray>",
				" ",
				"<gray>Người chơi: <#7fd36b>" + formatNumber(match.getLobbyPlayers().size()) + "</#7fd36b></gray>"
			)));

		inventory.setItem(31, buildInfoItem(Material.MAP,
			"<yellow>ℹ</yellow> Tổng quan",
			List.of(
				"<gray>Speedrunner: " + formatSpeedrunnerColored(match.getSettings()) + "</gray>",
				"<gray>Viền tối đa: "
					+ formatNumericValue(settings.getWorldBorderLimit(), defaults.getWorldBorderLimit()) + "</gray>",
				" ",
				"<gray>Thẻ bật: " + formatCountValue(enabledCards, defaultEnabledCards) + "</gray>",
				"<gray>Bộ đồ: " + kitText + "</gray>"
			)));

		inventory.setItem(34, buildActionItem(Material.PLAYER_HEAD,
			"<yellow>ℹ</yellow> Người chơi",
			buildMatchPlayersLore(match), ACTION_MATCH_INFO_PLAYERS, "0"));

		List<String> settingsOverview = new ArrayList<>();
		settingsOverview.add("<gray>● Hiển thị: " + formatVisibilityColored(settings.getVisibility()) + "</gray>");
		settingsOverview.add("<gray>● Speedrunner: " + formatSpeedrunnerColored(settings) + "</gray>");
		settingsOverview.add("<gray>● Seed: " + seedText + "</gray>");
		settingsOverview.add("<gray>● Phiên bản: "
			+ formatGenVersionValue(settings.getWorldGenerationVersion(), defaults.getWorldGenerationVersion())
			+ "</gray>");
		settingsOverview.add("<gray>● Thời gian: "
			+ formatWorldTimeColored(settings.getWorldTime(), defaults.getWorldTime()) + "</gray>");
		settingsOverview.add("<gray>● Thời tiết: "
			+ formatWorldWeatherColored(settings.getWorldWeather(), defaults.getWorldWeather()) + "</gray>");
		settingsOverview.add("<gray>● Độ khó: "
			+ formatWorldDifficultyColored(settings.getWorldDifficulty(), defaults.getWorldDifficulty()) + "</gray>");
		settingsOverview.add("<gray>● Giữ đồ: "
			+ formatKeepInventoryColored(settings.isKeepInventory(), defaults.isKeepInventory()) + "</gray>");
		settingsOverview.add(" ");
		settingsOverview.add("<gray>● Viền tối đa: "
			+ formatNumericValue(settings.getWorldBorderLimit(), defaults.getWorldBorderLimit()) + "</gray>");
		settingsOverview.add("<gray>● Viền ban đầu: "
			+ formatNumericValue(settings.getStartingBorder(), defaults.getStartingBorder()) + "</gray>");
		settingsOverview.add("<gray>● Thẻ bật: " + formatCountValue(enabledCards, defaultEnabledCards) + "</gray>");
		settingsOverview.add("<gray>● Bộ đồ: " + kitText + "</gray>");

		if (isOwner) {
			inventory.setItem(10, buildActionItem(Material.PAPER,
				"<yellow>ℹ</yellow> Mời người chơi",
				List.of(
					"<gray>Mời người chơi vào phòng.</gray>",
					" ",
					"<gray>Quyền: <white>Chủ phòng</white></gray>"
				), ACTION_MATCH_INFO_INVITE, null));
			if (match.getState() == MatchState.LOBBY) {
				inventory.setItem(28, buildActionItem(Material.LIME_WOOL,
					"<green>✔</green> Bắt đầu trận",
					List.of(
						"<gray>Tạo thế giới và bắt đầu trận đấu.</gray>",
						" ",
						"<gray>Quyền: <white>Chủ phòng</white></gray>"
					), ACTION_MATCH_INFO_START, null));
			} else {
				inventory.setItem(28, buildInfoItem(Material.GRAY_DYE,
					"<yellow>ℹ</yellow> Bắt đầu trận",
					List.of(
						"<gray>Chỉ có thể bắt đầu ở sảnh.</gray>",
						" ",
						"<gray>Trạng thái: " + formatStateColored(match.getState()) + "</gray>"
					)));
			}
			List<String> settingsLore = new ArrayList<>();
			settingsLore.add("<gray>Điều chỉnh cài đặt phòng.</gray>");
			settingsLore.add(" ");
			settingsLore.add("<gray>Tổng quan cài đặt:</gray>");
			settingsLore.addAll(settingsOverview);
			settingsLore.add(" ");
			settingsLore.add("<gray>Quyền: <white>Chủ phòng</white></gray>");
			inventory.setItem(12, buildActionItem(Material.COMPARATOR,
				"<yellow>ℹ</yellow> Cài đặt trận",
				settingsLore, ACTION_MATCH_INFO_SETTINGS, null));
			inventory.setItem(14, buildActionItem(Material.EMERALD,
				"<yellow>ℹ</yellow> Thẻ dùng trong trận",
				List.of(
					"<gray>Bật/tắt thẻ cho trận đấu.</gray>",
					" ",
					"<gray>Quyền: <white>Chủ phòng</white></gray>"
				), ACTION_MATCH_INFO_CARDS, "0"));
			inventory.setItem(16, buildActionItem(Material.CHEST,
				"<yellow>ℹ</yellow> Bộ đồ speedrunner",
				List.of(
					"<gray>Bộ đồ hiện tại: " + kitText + "</gray>",
					"<gray>Mặc định: " + defaultKitText + "</gray>",
					" ",
					"<gray>Bấm để chọn bộ đồ.</gray>",
					" ",
					"<gray>Quyền: <white>Chủ phòng</white></gray>"
				), ACTION_MATCH_INFO_KIT, null));
			inventory.setItem(46, buildActionItem(Material.RED_WOOL,
				"<red>❌</red> Giải tán phòng",
				List.of(
					"<gray>Rời khỏi phòng và giải tán phòng đấu.</gray>",
					" ",
					"<gray>Dùng: " + UiColors.command("/match leave") + "</gray>"
				), ACTION_MATCH_INFO_LEAVE, null));
		} else {
			inventory.setItem(10, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Mời người chơi",
				List.of(
					"<gray>Chỉ chủ phòng mới có thể mời.</gray>",
					" ",
					"<gray>Trạng thái: <white>Chỉ xem</white></gray>"
				)));
			inventory.setItem(28, buildInfoItem(Material.GRAY_DYE,
				"<yellow>ℹ</yellow> Bắt đầu trận",
				List.of(
					"<gray>Chỉ chủ phòng mới được bắt đầu.</gray>",
					" ",
					"<gray>Trạng thái: <white>Chỉ xem</white></gray>"
				)));
			List<String> settingsViewLore = new ArrayList<>();
			settingsViewLore.add("<gray>Chỉ chủ phòng mới có thể chỉnh.</gray>");
			settingsViewLore.add(" ");
			settingsViewLore.addAll(settingsOverview);
			settingsViewLore.add(" ");
			settingsViewLore.add("<gray>Trạng thái: <white>Chỉ xem</white></gray>");
			inventory.setItem(12, buildInfoItem(Material.COMPARATOR,
				"<yellow>ℹ</yellow> Cài đặt trận",
				settingsViewLore));
			inventory.setItem(14, buildActionItem(Material.EMERALD,
				"<yellow>ℹ</yellow> Thẻ dùng trong trận",
				List.of(
					"<gray>Xem danh sách thẻ đã bật.</gray>",
					" ",
					"<gray>Trạng thái: <white>Chỉ xem</white></gray>"
				), ACTION_MATCH_INFO_CARDS, "0"));
			inventory.setItem(16, buildInfoItem(Material.CHEST,
				"<yellow>ℹ</yellow> Bộ đồ speedrunner",
				List.of(
					"<gray>Bộ đồ hiện tại: " + kitText + "</gray>",
					"<gray>Mặc định: " + defaultKitText + "</gray>",
					" ",
					"<gray>Trạng thái: <white>Chỉ xem</white></gray>"
				)));
			inventory.setItem(46, buildActionItem(Material.OAK_DOOR,
				"<yellow>ℹ</yellow> Rời phòng",
				List.of(
					"<gray>Rời khỏi phòng hiện tại.</gray>",
					" ",
					"<gray>Dùng: " + UiColors.command("/match leave") + "</gray>"
				), ACTION_MATCH_INFO_LEAVE, null));
		}

		inventory.setItem(49, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng bảng thông tin.</gray>"), ACTION_MATCH_INFO_CLOSE, null));
	}

	private void fillCardsAdjustMenu(Player player, Match match, Inventory inventory, int page, int totalPages) {
		boolean isOwner = match.getOwnerId().equals(player.getUniqueId());
		List<CardDefinition> enabled = cardService.getEnabledCards(match);
		List<CardDefinition> available = new ArrayList<>();
		if (isOwner) {
			for (CardDefinition card : cardService.getCardPool()) {
				if (!enabled.contains(card)) {
					available.add(card);
				}
			}
		}

		setCardsAdjustSeparator(inventory);
		fillCardsAdjustList(inventory, available, AVAILABLE_CARD_SLOTS, page, isOwner,
			ACTION_CARDS_ADJUST_ENABLE);
		fillCardsAdjustList(inventory, enabled, ENABLED_CARD_SLOTS, page, isOwner,
			ACTION_CARDS_ADJUST_DISABLE);

		inventory.setItem(46, buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Thẻ khả dụng",
			List.of("<gray>Chưa bật: <white>" + available.size() + "</white></gray>")));
		inventory.setItem(52, buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Thẻ đang bật",
			List.of("<gray>Đang bật: <white>" + enabled.size() + "</white></gray>")));

		if (page > 0) {
			inventory.setItem(45, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_CARDS_ADJUST_PREV,
				String.valueOf(page - 1)));
		}
		if (page < totalPages - 1) {
			inventory.setItem(53, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang tiếp theo.</gray>"), ACTION_CARDS_ADJUST_NEXT,
				String.valueOf(page + 1)));
		}

		inventory.setItem(49, buildActionItem(Material.BARRIER,
			"<red>❌</red> Quay lại",
			List.of("<gray>Quay về thông tin phòng.</gray>"), ACTION_CARDS_ADJUST_CLOSE, null));
		if (!isOwner) {
			inventory.setItem(50, buildInfoItem(Material.GRAY_DYE,
				"<yellow>ℹ</yellow> Chế độ xem",
				List.of("<gray>Chỉ chủ phòng mới được chỉnh.</gray>")));
		}
	}

	private void fillCardsInfoMenu(List<CardDefinition> pool, Inventory inventory, int page, int totalPages) {
		if (pool == null || inventory == null) {
			return;
		}
		int start = page * CARDS_INFO_PAGE_SIZE;
		int end = Math.min(pool.size(), start + CARDS_INFO_PAGE_SIZE);
		int slot = 0;
		for (int i = start; i < end; i++) {
			inventory.setItem(slot++, cardService.buildCardInfoItem(pool.get(i)));
		}
		if (pool.isEmpty()) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Chưa có thẻ",
				List.of("<gray>Danh sách thẻ đang trống.</gray>")));
		}

		if (page > 0) {
			inventory.setItem(45, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_CARDS_INFO_PREV,
				String.valueOf(page - 1)));
		}
		if (page < totalPages - 1) {
			inventory.setItem(53, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang tiếp theo.</gray>"), ACTION_CARDS_INFO_NEXT,
				String.valueOf(page + 1)));
		}

		inventory.setItem(49, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng danh sách thẻ.</gray>"), ACTION_CARDS_INFO_CLOSE, null));
	}

	private void fillProfileMenu(Player viewer, PlayerProfile profile, Inventory inventory, UUID targetId) {
		if (viewer == null || profile == null || inventory == null) {
			return;
		}
		OfflinePlayer target = targetId == null ? viewer : Bukkit.getOfflinePlayer(targetId);
		inventory.setItem(13, buildProfileSummaryItem(target, profile));
		inventory.setItem(15, buildHistorySummaryItem(profile));
		inventory.setItem(22, buildActionItem(Material.BARRIER,
			"<red>❌</red> Đóng",
			List.of("<gray>Đóng hồ sơ.</gray>"), ACTION_PROFILE_CLOSE, null));
	}

	private void fillProfileHistoryMenu(PlayerProfile profile, Inventory inventory, int page, int totalPages) {
		if (profile == null || inventory == null) {
			return;
		}
		List<MatchHistoryEntry> history = profile.getMatchHistory();
		List<MatchHistoryEntry> ordered = new ArrayList<>(history.size());
		for (int i = history.size() - 1; i >= 0; i--) {
			ordered.add(history.get(i));
		}
		if (ordered.isEmpty()) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Chưa có trận đấu",
				List.of("<gray>Hãy chơi để ghi lại lịch sử.</gray>")));
		}

		int start = page * HISTORY_PAGE_SIZE;
		int end = Math.min(ordered.size(), start + HISTORY_PAGE_SIZE);
		int slot = 0;
		for (int i = start; i < end; i++) {
			inventory.setItem(slot++, buildHistoryEntryItem(ordered.get(i)));
		}

		if (page > 0) {
			inventory.setItem(45, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_PROFILE_HISTORY_PREV,
				String.valueOf(page - 1)));
		}
		if (page < totalPages - 1) {
			inventory.setItem(53, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang tiếp theo.</gray>"), ACTION_PROFILE_HISTORY_NEXT,
				String.valueOf(page + 1)));
		}

		inventory.setItem(49, buildActionItem(Material.BARRIER,
			"<red>❌</red> Quay lại",
			List.of("<gray>Quay về hồ sơ.</gray>"), ACTION_PROFILE_HISTORY_BACK, null));
	}

	private void fillMatchPlayersMenu(Match match, List<UUID> players, Inventory inventory, int page, int totalPages) {
		if (match == null || inventory == null || players == null) {
			return;
		}
		int start = page * MATCH_PLAYERS_PAGE_SIZE;
		int end = Math.min(players.size(), start + MATCH_PLAYERS_PAGE_SIZE);
		int slot = 0;
		for (int i = start; i < end; i++) {
			UUID playerId = players.get(i);
			String name = resolvePlayerName(playerId);
			inventory.setItem(slot++, buildMatchPlayerItem(playerId, name));
		}
		if (players.isEmpty()) {
			inventory.setItem(22, buildInfoItem(Material.PAPER,
				"<yellow>ℹ</yellow> Không có người chơi",
				List.of("<gray>Phòng đang trống.</gray>")));
		}

		if (page > 0) {
			inventory.setItem(45, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang trước",
				List.of("<gray>Xem trang trước.</gray>"), ACTION_MATCH_PLAYERS_PREV,
				String.valueOf(page - 1)));
		}
		if (page < totalPages - 1) {
			inventory.setItem(53, buildActionItem(Material.ARROW,
				"<yellow>ℹ</yellow> Trang sau",
				List.of("<gray>Xem trang tiếp theo.</gray>"), ACTION_MATCH_PLAYERS_NEXT,
				String.valueOf(page + 1)));
		}

		inventory.setItem(49, buildActionItem(Material.BARRIER,
			"<red>❌</red> Quay lại",
			List.of("<gray>Quay về thông tin phòng.</gray>"), ACTION_MATCH_PLAYERS_BACK, null));

		inventory.setItem(47, buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Tổng số",
			List.of("<gray>Người chơi: <white>" + formatNumber(players.size()) + "</white></gray>")));
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
		updateLobbyItems(player);
		openMatchInfoMenu(player);
	}

	private void handleMyMatch(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			Text.msg(player, "<red>❌</red> Bạn chưa ở trong phòng nào.");
			return;
		}
		openMatchInfoMenu(player);
	}

	private void handleJoin(Player player, String matchId) {
		if (matchId == null || matchId.isBlank()) {
			return;
		}
		LobbyActionResult result = lobbyManager.join(player.getUniqueId(), matchId);
		Text.msg(player, result.getMessage());
		if (result.isSuccess() && result.getMatch() != null) {
			updateLobbyItems(player);
			openMatchInfoMenu(player);
		}
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
			sendInviteMessage(target, match);
		}
		openInviteMenu(player, match);
	}

	private void sendInviteMessage(Player target, Match match) {
		if (target == null || match == null) {
			return;
		}
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<green>✔</green> Bạn vừa nhận lời mời vào phòng <yellow>" + match.getId() + "</yellow>."));
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<gray>Chọn: </gray>"
			+ "<green><click:run_command:'/m accept'><hover:show_text:'<gray>Bấm để chấp nhận</gray>'><b>[CHẤP NHẬN]</b></hover></click></green> "
			+ "<red><click:run_command:'/m decline'><hover:show_text:'<gray>Bấm để từ chối</gray>'><b>[TỪ CHỐI]</b></hover></click></red> "
			+ "<gray>(</gray>" + UiColors.command("/m accept") + "<gray> | </gray>"
			+ UiColors.command("/m decline") + "<gray>)</gray>"));
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
		openSettingsMenu(player, match, 0);
	}

	private void handleOpenCards(Player player) {
		Match match = requireSpeedrunnerMatch(player);
		if (match == null) {
			return;
		}
		openCardsMenu(player, match);
	}

	private void handleMatchInfoInvite(Player player, String matchId) {
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

	private void handleMatchInfoSettings(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được chỉnh cài đặt.");
			return;
		}
		openSettingsMenu(player, match, 0);
	}

	private void handleMatchInfoCards(Player player, String matchId, String target) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		int page = parsePage(target, 0);
		openCardsAdjustMenu(player, match, page);
	}

	private void handleMatchInfoKit(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được chỉnh bộ đồ.");
			return;
		}
		openSettingsMenu(player, match, getSettingsPageForRow(SETTINGS_ROW_KIT_INDEX));
	}

	private void handleMatchInfoStart(Player player, String matchId) {
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
			openMatchInfoMenu(player);
			return;
		}
		if (match.getLobbyPlayers().size() < 2) {
			Text.msg(player, "<red>❌</red> Cần ít nhất <aqua>2</aqua> người chơi để bắt đầu.");
			openMatchInfoMenu(player);
			return;
		}
		player.closeInventory();
		boolean started = matchManager.startMatch(match);
		if (started) {
			Text.msg(player, "<green>✔</green> Đang tạo thế giới cho trận đấu...");
			return;
		}
		Text.msg(player, "<red>❌</red> Không thể bắt đầu trận đấu lúc này.");
		openMatchInfoMenu(player);
	}

	private void handleMatchInfoPlayers(Player player, String matchId, String target) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		int page = parsePage(target, 0);
		openMatchPlayersMenu(player, match, page);
	}

	private void handleMatchPlayersPage(Player player, String matchId, String target) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			player.closeInventory();
			return;
		}
		int page = parsePage(target, 0);
		openMatchPlayersMenu(player, match, page);
	}

	private void handleMatchPlayersBack(Player player, String matchId) {
		openMatchInfoMenu(player);
	}

	private void handleOpenMatchPlayerProfile(Player player, String target) {
		if (player == null || target == null || target.isBlank()) {
			return;
		}
		UUID targetId;
		try {
			targetId = UUID.fromString(target);
		} catch (IllegalArgumentException ex) {
			return;
		}
		openProfileMenu(player, targetId);
	}

	private void handleMatchInfoLeave(Player player) {
		if (player == null) {
			return;
		}
		LobbyActionResult result = lobbyManager.leave(player.getUniqueId());
		Text.msg(player, result.getMessage());
		updateLobbyItems(player);
		openMainMenu(player);
	}

	private void handleAdjustCard(Player player, String matchId, String target, boolean enable) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		if (!match.getOwnerId().equals(player.getUniqueId())) {
			Text.msg(player, "<red>❌</red> Chỉ chủ phòng mới được chỉnh thẻ.");
			return;
		}
		String[] parts = splitCardTarget(target);
		String cardId = parts[0];
		int page = parsePage(parts[1], 0);
		if (cardId == null || cardId.isBlank()) {
			return;
		}
		cardService.setCardEnabled(match, cardId, enable);
		openCardsAdjustMenu(player, match, page);
	}

	private void handleOpenCardsAdjustPage(Player player, String matchId, String target) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Không tìm thấy phòng đấu.");
			return;
		}
		int page = parsePage(target, 0);
		openCardsAdjustMenu(player, match, page);
	}

	private void handleCardsAdjustClose(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			player.closeInventory();
			return;
		}
		openMatchInfoMenu(player);
	}

	private void handleOpenSpeedrunnerMenu(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		openSpeedrunnerMenu(player, match);
	}

	private void handleOpenSettingsPage(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		int page = parsePage(target, getSettingsPage(player));
		openSettingsMenu(player, match, page);
	}

	private void handleBackToLobby(Player player, String matchId) {
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			openMainMenu(player);
			return;
		}
		openMatchInfoMenu(player);
	}

	private void handleVisibilitySet(Player player, String matchId, MatchVisibility visibility) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null || visibility == null) {
			return;
		}
		match.getSettings().setVisibility(visibility);
		Text.msg(player, "<green>✔</green> Đã đổi hiển thị sang <yellow>" + formatVisibility(visibility) + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetVisibility(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setVisibility(defaults.getVisibility());
		Text.msg(player, "<green>✔</green> Đã khôi phục hiển thị mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetSpeedrunner(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setSpeedrunnerSelection(defaults.getSpeedrunnerSelection());
		match.getSettings().setSpeedrunnerId(defaults.getSpeedrunnerId());
		Text.msg(player, "<green>✔</green> Đã khôi phục speedrunner mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetSeed(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setWorldSeed(defaults.getWorldSeed());
		Text.msg(player, "<green>✔</green> Đã khôi phục seed mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleSeedClear(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setWorldSeed(null);
		Text.msg(player, "<green>✔</green> Đã xóa seed.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetWorldGenerationVersion(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setWorldGenerationVersion(defaults.getWorldGenerationVersion());
		Text.msg(player, "<green>✔</green> Đã khôi phục phiên bản tạo thế giới mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleWorldGenerationVersionSet(Player player, String matchId, String version) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null || version == null || version.isBlank()) {
			return;
		}
		String resolved = matchSettingsService.resolveWorldGenerationVersion(version);
		match.getSettings().setWorldGenerationVersion(resolved);
		Text.msg(player, "<green>✔</green> Phiên bản tạo thế giới: <yellow>" + resolved + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleWorldGenerationVersionPrompt(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		genVersionInputs.put(player.getUniqueId(), match.getId());
		player.closeInventory();
		Text.msg(player, "<yellow>ℹ</yellow> Nhập phiên bản vào chat. Gõ <white>huy</white> để hủy.");
	}

	private void handleResetWorldTime(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setWorldTime(defaults.getWorldTime());
		Text.msg(player, "<green>✔</green> Đã khôi phục thời gian thế giới mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleWorldTimeSet(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		WorldTimeSetting time = MatchSettingsService.parseWorldTime(target, match.getSettings().getWorldTime());
		match.getSettings().setWorldTime(time);
		Text.msg(player, "<green>✔</green> Thời gian: <yellow>" + formatWorldTime(time) + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetWorldWeather(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setWorldWeather(defaults.getWorldWeather());
		Text.msg(player, "<green>✔</green> Đã khôi phục thời tiết mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleWorldWeatherSet(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		WorldWeatherSetting weather = MatchSettingsService.parseWorldWeather(
			target, match.getSettings().getWorldWeather());
		match.getSettings().setWorldWeather(weather);
		Text.msg(player, "<green>✔</green> Thời tiết: <yellow>" + formatWorldWeather(weather) + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetWorldDifficulty(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setWorldDifficulty(defaults.getWorldDifficulty());
		Text.msg(player, "<green>✔</green> Đã khôi phục độ khó mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleWorldDifficultySet(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		WorldDifficultySetting difficulty = MatchSettingsService.parseWorldDifficulty(
			target, match.getSettings().getWorldDifficulty());
		match.getSettings().setWorldDifficulty(difficulty);
		Text.msg(player, "<green>✔</green> Độ khó: <yellow>" + formatWorldDifficulty(difficulty) + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetKeepInventory(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setKeepInventory(defaults.isKeepInventory());
		Text.msg(player, "<green>✔</green> Đã khôi phục giữ đồ mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleKeepInventorySet(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		boolean keepInventory = MatchSettingsService.parseKeepInventory(target, match.getSettings().isKeepInventory());
		match.getSettings().setKeepInventory(keepInventory);
		Text.msg(player, "<green>✔</green> Giữ đồ: <yellow>" + formatKeepInventory(keepInventory) + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetKit(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		String defaultKitId = resolveDefaultKitId(defaults);
		kitService.applyKit(match.getSettings(), defaultKitId);
		Text.msg(player, "<green>✔</green> Đã khôi phục bộ đồ mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleOpenKitPreview(Player player, String matchId, String kitId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null || kitId == null || kitId.isBlank()) {
			return;
		}
		SpeedrunnerKitDefinition kit = kitService.getKit(kitId);
		if (kit == null) {
			Text.msg(player, "<red>❌</red> Bộ đồ không hợp lệ.");
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		kitPreviewPages.put(player.getUniqueId(), getSettingsPage(player));
		openKitPreviewMenu(player, match, kit);
	}

	private void handleOpenKitPage(Player player, String matchId, String target) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		int page = parsePage(target, getKitPage(player));
		setKitPage(player, page);
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleSelectKit(Player player, String matchId, String kitId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null || kitId == null || kitId.isBlank()) {
			return;
		}
		SpeedrunnerKitDefinition kit = kitService.getKit(kitId);
		if (kit == null) {
			Text.msg(player, "<red>❌</red> Bộ đồ không hợp lệ.");
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		kitService.applyKit(match.getSettings(), kitId);
		Text.msg(player, "<green>✔</green> Đã chọn bộ đồ <yellow>" + kit.getName() + "</yellow>.");
		openSettingsMenu(player, match, consumeKitPreviewPage(player));
	}

	private void handleBackFromKitPreview(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		openSettingsMenu(player, match, consumeKitPreviewPage(player));
	}

	private void handleResetBorder(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		int limit = defaults.getWorldBorderLimit();
		match.getSettings().setWorldBorderLimit(limit);
		if (match.getSettings().getStartingBorder() > limit) {
			match.getSettings().setStartingBorder(limit);
		}
		Text.msg(player, "<green>✔</green> Đã khôi phục viền tối đa mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetStartBorder(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		int next = defaults.getStartingBorder();
		int max = Math.max(5, match.getSettings().getWorldBorderLimit());
		match.getSettings().setStartingBorder(clamp(next, 5, max));
		Text.msg(player, "<green>✔</green> Đã khôi phục viền ban đầu mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleResetCards(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings defaults = matchSettingsService.createDefaults();
		match.getSettings().setEnabledCardIds(new ArrayList<>(defaults.getEnabledCardIds()));
		Text.msg(player, "<green>✔</green> Đã khôi phục thẻ mặc định.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleSpeedrunnerRandom(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setSpeedrunnerSelection(SpeedrunnerSelection.RANDOM);
		match.getSettings().setSpeedrunnerId(null);
		Text.msg(player, "<green>✔</green> Đã chọn speedrunner ngẫu nhiên.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	private void handleSpeedrunnerMe(Player player, String matchId) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		match.getSettings().setSpeedrunnerSelection(SpeedrunnerSelection.SPECIFIC);
		match.getSettings().setSpeedrunnerId(player.getUniqueId());
		Text.msg(player, "<green>✔</green> Đã chọn bạn làm speedrunner.");
		openSettingsMenu(player, match, getSettingsPage(player));
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
		openSettingsMenu(player, match, getSettingsPage(player));
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
		cardService.useKeptCard(player, match, index);
		openCardsMenu(player, match);
	}

	private void handleKeepCard(Player player, String matchId, String indexText) {
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
		cardService.keepCardFromPool(player, match, index);
		openCardsMenu(player, match);
	}

	private void handleUsePoolCard(Player player, String matchId, String indexText) {
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
		cardService.usePoolCard(player, match, index);
		openCardsMenu(player, match);
	}

	private void handleDiscardCard(Player player, String matchId, String indexText) {
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
		cardService.discardKeptCard(player, match, index);
		openCardsMenu(player, match);
	}

	private void handleBorderChange(Player player, String matchId, int delta) {
		Match match = requireSettingsMatch(player, matchId);
		if (match == null) {
			return;
		}
		MatchSettings settings = match.getSettings();
		int current = settings.getWorldBorderLimit();
		int next = clamp(current + delta, MatchSettings.WORLD_BORDER_LIMIT_MIN, MatchSettings.WORLD_BORDER_LIMIT_MAX);
		settings.setWorldBorderLimit(next);
		if (settings.getStartingBorder() > next) {
			settings.setStartingBorder(next);
		}
		Text.msg(player, "<green>✔</green> Viền tối đa: <yellow>" + next + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
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
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	public boolean hasSeedInput(Player player) {
		return player != null && seedInputs.containsKey(player.getUniqueId());
	}

	public boolean hasGenVersionInput(Player player) {
		return player != null && genVersionInputs.containsKey(player.getUniqueId());
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
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		if (input.equalsIgnoreCase("xoa")) {
			match.getSettings().setWorldSeed(null);
			Text.msg(player, "<green>✔</green> Đã xóa seed.");
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		match.getSettings().setWorldSeed(input);
		Text.msg(player, "<green>✔</green> Đã đặt seed.");
		openSettingsMenu(player, match, getSettingsPage(player));
	}

	public void handleGenVersionInput(Player player, String message) {
		if (player == null) {
			return;
		}
		String matchId = genVersionInputs.remove(player.getUniqueId());
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
			Text.msg(player, "<yellow>ℹ</yellow> Đã hủy nhập phiên bản.");
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		if (input.isBlank()) {
			Text.msg(player, "<red>❌</red> Phiên bản không hợp lệ.");
			openSettingsMenu(player, match, getSettingsPage(player));
			return;
		}
		String resolved = matchSettingsService.resolveWorldGenerationVersion(input);
		match.getSettings().setWorldGenerationVersion(resolved);
		Text.msg(player, "<green>✔</green> Đã đặt phiên bản tạo thế giới: <yellow>" + resolved + "</yellow>.");
		openSettingsMenu(player, match, getSettingsPage(player));
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

	private void updateLobbyItems(Player player) {
		if (lobbyItemService != null) {
			lobbyItemService.applyLobbyStateIfEligible(player);
		}
	}

	private String formatStateColored(MatchState state) {
		if (state == null) {
			return "<gray>Không rõ</gray>";
		}
		return switch (state) {
			case LOBBY -> "<#7fd36b>Chờ</#7fd36b>";
			case WORLD_CREATING -> "<#ffd166>Tạo thế giới</#ffd166>";
			case WORLD_READY -> "<#7fd36b>Sẵn sàng</#7fd36b>";
			case RUN_PENDING -> "<#ffd166>Chờ bắt đầu</#ffd166>";
			case RUNNING -> "<#36a8ff>Đang chạy</#36a8ff>";
			case ENDING -> "<#ff6b6b>Kết thúc</#ff6b6b>";
			case CLEANUP -> "<#ffd166>Dọn dẹp</#ffd166>";
			case FINISHED -> "<#9aa0a6>Đã xong</#9aa0a6>";
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

	private String formatVisibilityColored(MatchVisibility visibility) {
		if (visibility == null) {
			return "<gray>Không rõ</gray>";
		}
		return switch (visibility) {
			case PUBLIC -> "<#7fd36b>Công khai</#7fd36b>";
			case INVITE_ONLY -> "<#ffd166>Chỉ mời</#ffd166>";
			case PRIVATE -> "<#ff6b6b>Riêng tư</#ff6b6b>";
		};
	}

	private String formatSpeedrunnerColored(MatchSettings settings) {
		if (settings == null) {
			return "<gray>Chưa đặt</gray>";
		}
		if (settings.getSpeedrunnerSelection() == SpeedrunnerSelection.RANDOM) {
			return "<#36a8ff>Ngẫu nhiên</#36a8ff>";
		}
		UUID id = settings.getSpeedrunnerId();
		if (id == null) {
			return "<#ff6b6b>Chưa đặt</#ff6b6b>";
		}
		Player player = Bukkit.getPlayer(id);
		return player == null
			? "<#ff6b6b>Chưa đặt</#ff6b6b>"
			: "<#7fd36b>" + player.getName() + "</#7fd36b>";
	}

	private String formatSeedValue(String seed, String defaultSeed) {
		String trimmed = seed == null ? "" : seed.trim();
		if (trimmed.isEmpty()) {
			return "<#ff6b6b>Chưa đặt</#ff6b6b>";
		}
		String defaultTrimmed = defaultSeed == null ? "" : defaultSeed.trim();
		String color = trimmed.equals(defaultTrimmed) ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + trimmed + "</" + color + ">";
	}

	private String formatGenVersionValue(String version, String defaultVersion) {
		String trimmed = version == null ? "" : version.trim();
		if (trimmed.isEmpty()) {
			return "<#ff6b6b>Chưa đặt</#ff6b6b>";
		}
		String defaultTrimmed = defaultVersion == null ? "" : defaultVersion.trim();
		String color = trimmed.equalsIgnoreCase(defaultTrimmed) ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + trimmed + "</" + color + ">";
	}

	private String formatWorldTime(WorldTimeSetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case CYCLE -> "Chu kỳ";
			case MORNING -> "Buổi sáng";
			case NOON -> "Buổi trưa";
			case NIGHT -> "Ban đêm";
			case MIDNIGHT -> "Nửa đêm";
		};
	}

	private String formatWorldTimeColored(WorldTimeSetting setting, WorldTimeSetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldTime(setting) + "</" + color + ">";
	}

	private String formatWorldWeather(WorldWeatherSetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case CYCLE -> "Chu kỳ";
			case CLEAR -> "Trời quang";
			case RAIN -> "Mưa";
			case THUNDERSTORM -> "Giông";
		};
	}

	private String formatWorldWeatherColored(WorldWeatherSetting setting, WorldWeatherSetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldWeather(setting) + "</" + color + ">";
	}

	private String formatWorldDifficulty(WorldDifficultySetting setting) {
		if (setting == null) {
			return "Không rõ";
		}
		return switch (setting) {
			case PEACEFUL -> "Hòa bình";
			case EASY -> "Dễ";
			case MEDIUM -> "Trung bình";
			case HARD -> "Khó";
		};
	}

	private String formatWorldDifficultyColored(WorldDifficultySetting setting, WorldDifficultySetting defaultSetting) {
		if (setting == null) {
			return "<gray>Không rõ</gray>";
		}
		String color = setting == defaultSetting ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatWorldDifficulty(setting) + "</" + color + ">";
	}

	private String formatKeepInventory(boolean keepInventory) {
		return keepInventory ? "Bật" : "Tắt";
	}

	private String formatKeepInventoryColored(boolean keepInventory, boolean defaultKeepInventory) {
		String color = keepInventory == defaultKeepInventory ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatKeepInventory(keepInventory) + "</" + color + ">";
	}

	private String formatNumericValue(int value, int defaultValue) {
		String color = value == defaultValue ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatNumber(value) + "</" + color + ">";
	}

	private String formatCountValue(int value, int defaultValue) {
		if (value <= 0) {
			return "<#ff6b6b>" + formatNumber(value) + "</#ff6b6b>";
		}
		String color = value == defaultValue ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + formatNumber(value) + "</" + color + ">";
	}

	private String formatNumber(int value) {
		NumberFormat format = NumberFormat.getIntegerInstance(Locale.of("vi", "VN"));
		return format.format(value);
	}

	private String resolveDefaultKitId(MatchSettings defaults) {
		if (defaults == null) {
			return kitService.getDefaultKitId();
		}
		String kitId = defaults.getSpeedrunnerKitId();
		return kitId == null || kitId.isBlank() ? kitService.getDefaultKitId() : kitId;
	}

	private String formatKitNameColored(MatchSettings settings, String defaultKitId) {
		if (settings == null) {
			return "<gray>Không rõ</gray>";
		}
		String kitId = settings.getSpeedrunnerKitId();
		if (kitId == null || kitId.isBlank()) {
			if (settings.getSpeedrunnerKit() != null && !settings.getSpeedrunnerKit().isEmpty()) {
				return "<#ffd166>Tùy chỉnh</#ffd166>";
			}
			return "<#ff6b6b>Không có</#ff6b6b>";
		}
		SpeedrunnerKitDefinition kit = kitService.getKit(kitId);
		if (kit == null) {
			return "<#ff6b6b>Không rõ</#ff6b6b>";
		}
		String resolvedDefaultId = defaultKitId == null || defaultKitId.isBlank()
			? kitService.getDefaultKitId()
			: defaultKitId;
		String color = kitId.equalsIgnoreCase(resolvedDefaultId) ? "#7fd36b" : "#ffd166";
		return "<" + color + ">" + kit.getName() + "</" + color + ">";
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

	private int getSettingsPageCount() {
		return (SETTINGS_TOTAL_ROWS + SETTINGS_ROWS_PER_PAGE - 1) / SETTINGS_ROWS_PER_PAGE;
	}

	private int getSettingsPageForRow(int rowIndex) {
		int page = rowIndex <= 0 ? 0 : rowIndex / SETTINGS_ROWS_PER_PAGE;
		return clamp(page, 0, Math.max(0, getSettingsPageCount() - 1));
	}

	private int getSettingsPage(Player player) {
		if (player == null) {
			return 0;
		}
		return settingsPages.getOrDefault(player.getUniqueId(), 0);
	}

	private int getKitPage(Player player) {
		if (player == null) {
			return 0;
		}
		return kitPages.getOrDefault(player.getUniqueId(), 0);
	}

	private void setKitPage(Player player, int page) {
		if (player == null) {
			return;
		}
		kitPages.put(player.getUniqueId(), Math.max(0, page));
	}

	private int consumeKitPreviewPage(Player player) {
		if (player == null) {
			return 0;
		}
		Integer page = kitPreviewPages.remove(player.getUniqueId());
		return page == null ? getSettingsPage(player) : page;
	}

	private int parseDelta(String target, int fallback) {
		if (target == null || target.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(target.trim());
		} catch (NumberFormatException ex) {
			return fallback;
		}
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
				"<gray>● Chủ phòng: <#7fd36b>" + ownerName + "</#7fd36b></gray>",
				"<gray>● Hiển thị: " + formatVisibilityColored(match.getSettings().getVisibility()) + "</gray>",
				"<gray>● Người chơi: <white>" + formatNumber(match.getLobbyPlayers().size()) + "</white></gray>",
				" ",
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

	private ItemStack buildProfileSummaryItem(OfflinePlayer player, PlayerProfile profile) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		String bestTime = formatDuration(profile.getBestTimeMillis());
		List<String> lore = List.of(
			"<gray>Thành tích tốt nhất: <white>" + bestTime + "</white></gray>",
			"<gray>Số lần thắng: <white>" + formatNumber(profile.getTotalWins()) + "</white></gray>",
			"<gray>Số lần thua: <white>" + formatNumber(profile.getTotalLosses()) + "</white></gray>",
			"<gray>Số lần chạy: <white>" + formatNumber(profile.getRunTimesMillis().size()) + "</white></gray>"
		);
		if (meta instanceof SkullMeta skullMeta) {
			skullMeta.setOwningPlayer(player);
			skullMeta.displayName(mmNoItalic("<yellow>Hồ sơ cá nhân</yellow>"));
			skullMeta.lore(mmNoItalic(lore));
			item.setItemMeta(skullMeta);
		} else if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>Hồ sơ cá nhân</yellow>"));
			meta.lore(mmNoItalic(lore));
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildHistorySummaryItem(PlayerProfile profile) {
		List<MatchHistoryEntry> history = profile.getMatchHistory();
		List<String> lore = new ArrayList<>();
		int shown = 0;
		for (int i = history.size() - 1; i >= 0 && shown < 5; i--) {
			MatchHistoryEntry entry = history.get(i);
			long timeMillis = entry.getEndedAtMillis() > 0 ? entry.getEndedAtMillis()
				: entry.getStartedAtMillis();
			lore.add("<gray>● <white>" + safeText(entry.getMatchId(), "?") + "</white> - "
				+ formatHistoryOutcome(entry) + "</gray>");
			lore.add("<gray>  " + formatRelativeTime(timeMillis) + "</gray>");
			shown++;
		}
		if (lore.isEmpty()) {
			lore.add("<gray>Chưa có trận đấu.</gray>");
		}
		lore.add("<gray>Bấm để xem đầy đủ.</gray>");
		return buildActionItem(Material.BOOK,
			"<yellow>Lịch sử trận đấu</yellow>",
			lore, ACTION_PROFILE_HISTORY_OPEN, null);
	}

	private ItemStack buildHistoryEntryItem(MatchHistoryEntry entry) {
		String matchId = safeText(entry.getMatchId(), "Không rõ");
		String role = formatHistoryRole(entry.getRole());
		String outcome = formatHistoryOutcome(entry);
		String winner = formatHistoryWinner(entry.getWinner());
		String duration = formatDuration(entry.getDurationMillis());
		String timestamp = formatTimestamp(entry.getEndedAtMillis() > 0 ? entry.getEndedAtMillis()
			: entry.getStartedAtMillis());
		return buildInfoItem(Material.PAPER,
			"<yellow>Trận <white>" + matchId + "</white></yellow>",
			List.of(
				"<gray>Vai trò: <white>" + role + "</white></gray>",
				"<gray>Kết quả: " + outcome + "</gray>",
				"<gray>Bên thắng: <white>" + winner + "</white></gray>",
				"<gray>Thời gian: <white>" + duration + "</white></gray>",
				"<gray>Thời điểm: <white>" + timestamp + "</white></gray>"
			));
	}

	private void handleOpenHistoryPage(Player player, String target) {
		int page = parsePage(target, 0);
		openProfileHistoryMenu(player, page);
	}

	private void handleOpenCardsInfoPage(Player player, String target) {
		int page = parsePage(target, 0);
		openCardsInfoMenu(player, page);
	}

	private int parsePage(String target, int fallback) {
		if (target == null || target.isBlank()) {
			return fallback;
		}
		try {
			return Integer.parseInt(target);
		} catch (NumberFormatException ex) {
			return fallback;
		}
	}

	private UUID resolveProfileTarget(Player viewer) {
		if (viewer == null) {
			return null;
		}
		UUID viewerId = viewer.getUniqueId();
		return profileViewTargets.getOrDefault(viewerId, viewerId);
	}

	private List<UUID> getSortedMatchPlayers(Match match) {
		if (match == null) {
			return List.of();
		}
		List<UUID> players = new ArrayList<>(match.getLobbyPlayers());
		players.sort((left, right) -> resolvePlayerName(left).compareToIgnoreCase(resolvePlayerName(right)));
		return players;
	}

	private List<String> buildMatchPlayersLore(Match match) {
		List<UUID> players = getSortedMatchPlayers(match);
		List<String> lore = new ArrayList<>();
		lore.add("<gray>Tổng số: <white>" + formatNumber(players.size()) + "</white></gray>");
		lore.add(" ");
		lore.add("<gray>Danh sách:</gray>");
		if (players.isEmpty()) {
			lore.add("<gray>● <white>Chưa có người chơi</white></gray>");
		} else {
			int maxShown = 8;
			int shown = Math.min(maxShown, players.size());
			for (int i = 0; i < shown; i++) {
				lore.add("<gray>● <white>" + resolvePlayerName(players.get(i)) + "</white></gray>");
			}
			if (players.size() > maxShown) {
				int remaining = players.size() - maxShown;
				lore.add("<gray>● <white>+" + formatNumber(remaining) + " người nữa</white></gray>");
			}
		}
		lore.add(" ");
		lore.add("<gray>Bấm để xem đầy đủ.</gray>");
		return lore;
	}

	private ItemStack buildMatchPlayerItem(UUID playerId, String name) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		if (meta instanceof SkullMeta skullMeta) {
			OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
			skullMeta.setOwningPlayer(offline);
			skullMeta.displayName(mmNoItalic("<yellow>" + name + "</yellow>"));
			skullMeta.lore(mmNoItalic(List.of(
				"<gray>Bấm để xem hồ sơ.</gray>"
			)));
			skullMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MATCH_PLAYER_PROFILE);
			skullMeta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, playerId.toString());
			item.setItemMeta(skullMeta);
		} else if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>" + name + "</yellow>"));
			meta.lore(mmNoItalic(List.of("<gray>Bấm để xem hồ sơ.</gray>")));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MATCH_PLAYER_PROFILE);
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, playerId.toString());
			item.setItemMeta(meta);
		}
		return item;
	}

	private String resolvePlayerName(UUID playerId) {
		Player online = Bukkit.getPlayer(playerId);
		if (online != null && online.getName() != null && !online.getName().isBlank()) {
			return online.getName();
		}
		OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
		String name = offline == null ? null : offline.getName();
		return name == null || name.isBlank() ? "Người chơi" : name;
	}

	private int getHistoryPageCount(int totalEntries) {
		if (totalEntries <= 0) {
			return 0;
		}
		return (totalEntries + HISTORY_PAGE_SIZE - 1) / HISTORY_PAGE_SIZE;
	}

	private int getMatchPlayersPageCount(int totalEntries) {
		if (totalEntries <= 0) {
			return 0;
		}
		return (totalEntries + MATCH_PLAYERS_PAGE_SIZE - 1) / MATCH_PLAYERS_PAGE_SIZE;
	}

	private int getCardsInfoPageCount(int totalEntries) {
		if (totalEntries <= 0) {
			return 0;
		}
		return (totalEntries + CARDS_INFO_PAGE_SIZE - 1) / CARDS_INFO_PAGE_SIZE;
	}

	private int getCardsAdjustPageCount(Match match, boolean isOwner) {
		if (match == null) {
			return 0;
		}
		int enabledCount = cardService.getEnabledCards(match).size();
		int maxCount = enabledCount;
		if (isOwner) {
			int availableCount = cardService.getCardPool().size() - enabledCount;
			maxCount = Math.max(enabledCount, availableCount);
		}
		if (maxCount <= 0) {
			return 0;
		}
		return (maxCount + CARDS_ADJUST_PAGE_SIZE - 1) / CARDS_ADJUST_PAGE_SIZE;
	}

	private void setCardsAdjustSeparator(Inventory inventory) {
		ItemStack separator = buildInfoItem(Material.GRAY_STAINED_GLASS_PANE,
			" ",
			List.of(" "));
		int[] slots = new int[] { 4, 13, 22, 31, 40 };
		for (int slot : slots) {
			inventory.setItem(slot, separator);
		}
	}

	private void fillCardsAdjustList(Inventory inventory, List<CardDefinition> cards, int[] slots, int page,
			boolean allowEdit, String action) {
		int start = page * CARDS_ADJUST_PAGE_SIZE;
		for (int i = 0; i < slots.length; i++) {
			int index = start + i;
			if (index >= cards.size()) {
				continue;
			}
			CardDefinition card = cards.get(index);
			ItemStack item = cardService.buildCardInfoItem(card);
			if (allowEdit) {
				String target = card.getId() + "|" + page;
				item = decorateCardAction(item, action, target);
			}
			inventory.setItem(slots[i], item);
		}
	}

	private ItemStack decorateCardAction(ItemStack item, String action, String target) {
		if (item == null) {
			return null;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
		meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
		if (target != null) {
			meta.getPersistentDataContainer().set(targetKey, PersistentDataType.STRING, target);
		}
		item.setItemMeta(meta);
		return item;
	}

	private String[] splitCardTarget(String target) {
		if (target == null) {
			return new String[] { null, null };
		}
		String[] parts = target.split("\\|", 2);
		if (parts.length == 1) {
			return new String[] { parts[0], null };
		}
		return parts;
	}

	private String formatHistoryOutcome(MatchHistoryEntry entry) {
		return entry != null && entry.isWon() ? "<green>Thắng</green>" : "<red>Thua</red>";
	}

	private String formatHistoryRole(String role) {
		if (role == null) {
			return "Không rõ";
		}
		if (role.equalsIgnoreCase("SPEEDRUNNER")) {
			return "Speedrunner";
		}
		if (role.equalsIgnoreCase("HUNTER")) {
			return "Thợ săn";
		}
		return role;
	}

	private String formatHistoryWinner(MatchWinner winner) {
		if (winner == null) {
			return "Không rõ";
		}
		return switch (winner) {
			case SPEEDRUNNER -> "Speedrunner";
			case HUNTERS -> "Thợ săn";
		};
	}

	private String safeText(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}

	private String formatDuration(long millis) {
		if (millis <= 0L) {
			return "Chưa có";
		}
		long totalSeconds = millis / 1000L;
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format("%d:%02d", minutes, seconds);
	}

	private String formatTimestamp(long millis) {
		if (millis <= 0L) {
			return "Chưa rõ";
		}
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
			.withZone(ZoneId.systemDefault());
		return formatter.format(Instant.ofEpochMilli(millis));
	}

	private String formatRelativeTime(long millis) {
		if (millis <= 0L) {
			return "Không rõ";
		}
		long now = System.currentTimeMillis();
		long deltaSeconds = Math.max(0L, (now - millis) / 1000L);
		if (deltaSeconds < 60) {
			return "Vừa xong";
		}
		long minutes = deltaSeconds / 60L;
		if (minutes < 60) {
			return minutes + " phút trước";
		}
		long hours = minutes / 60L;
		if (hours < 24) {
			return hours + " giờ trước";
		}
		long days = hours / 24L;
		if (days < 7) {
			return days + " ngày trước";
		}
		long weeks = days / 7L;
		if (weeks < 4) {
			return weeks + " tuần trước";
		}
		long months = days / 30L;
		if (months < 12) {
			return months + " tháng trước";
		}
		long years = days / 365L;
		return years + " năm trước";
	}

	private OptionItem buildOption(Material icon, String title, List<String> details, String action, String target) {
		List<String> lore = new ArrayList<>();
		if (details != null && !details.isEmpty()) {
			lore.addAll(details);
		}
		lore.add(" ");
		lore.add("<gray>Bấm để chọn.</gray>");
		return new OptionItem(icon, title, lore, action, target);
	}

	private OptionItem buildPreviewOption(Material icon, String title, List<String> details, String action,
			String target) {
		List<String> lore = new ArrayList<>();
		if (details != null && !details.isEmpty()) {
			lore.addAll(details);
		}
		lore.add(" ");
		lore.add("<gray>Bấm để xem bộ đồ.</gray>");
		return new OptionItem(icon, title, lore, action, target);
	}

	private OptionItem buildPageOption(Material icon, String title, List<String> details, String action,
			String target) {
		List<String> lore = new ArrayList<>();
		if (details != null && !details.isEmpty()) {
			lore.addAll(details);
		}
		lore.add(" ");
		lore.add("<gray>Bấm để xem trang.</gray>");
		return new OptionItem(icon, title, lore, action, target);
	}

	private record OptionItem(Material icon, String title, List<String> lore, String action, String target) {
	}

	private record SettingRow(Material icon, String title, List<String> lore, String resetAction,
			List<OptionItem> options) {
	}

	public enum GuiType {
		MAIN,
		INVITE,
		SETTINGS,
		SPEEDRUNNER,
		KIT_PREVIEW,
		CARDS,
		CARDS_INFO,
		MATCH_INFO,
		MATCH_PLAYERS,
		CARDS_ADJUST,
		PROFILE,
		PROFILE_HISTORY
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
