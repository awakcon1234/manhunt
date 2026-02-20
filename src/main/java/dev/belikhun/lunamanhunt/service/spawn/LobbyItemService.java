package dev.belikhun.lunamanhunt.service.spawn;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.gui.GuiService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRules;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Objects;

public class LobbyItemService {
	private static final String ACTION_MATCH_LIST = "match_list";
	private static final String ACTION_PROFILE = "profile";
	private static final String ACTION_MATCH_INFO = "match_info";

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final GuiService guiService;
	private final NamespacedKey actionKey;

	public LobbyItemService(JavaPlugin plugin, LobbyManager lobbyManager, GuiService guiService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.guiService = Objects.requireNonNull(guiService, "guiService");
		this.actionKey = new NamespacedKey(plugin, "lm_lobby_item");
	}

	public void applyLobbyState(Player player) {
		if (player == null) {
			return;
		}
		World world = player.getWorld();
		if (isLobbyWorld(world) && world != null) {
			world.setGameRule(GameRules.PVP, false);
		}
		player.setGameMode(GameMode.ADVENTURE);
		player.setAllowFlight(true);
		if (!player.isFlying()) {
			player.setFlying(true);
		}
		PlayerInventory inventory = player.getInventory();
		inventory.setItem(0, buildMatchListItem());
		inventory.setItem(1, buildMatchInfoItem(player));
		inventory.setItem(8, buildProfileItem(player));
		player.getInventory().setHeldItemSlot(0);
	}

	public void applyLobbyStateIfEligible(Player player) {
		if (!shouldApplyLobbyState(player)) {
			return;
		}
		applyLobbyState(player);
	}

	public boolean handleInteract(Player player, ItemStack item) {
		if (player == null || item == null) {
			return false;
		}
		String action = getAction(item);
		if (action == null || action.isBlank()) {
			return false;
		}
		if (!shouldApplyLobbyState(player)) {
			Text.msg(player, "<red>❌</red> Bạn đang trong trận, không thể dùng vật phẩm này.");
			return true;
		}
		switch (action) {
			case ACTION_MATCH_LIST -> guiService.openMainMenu(player);
			case ACTION_MATCH_INFO -> guiService.openMatchInfoMenu(player);
			case ACTION_PROFILE -> guiService.openProfileMenu(player);
			default -> {
			}
		}
		return true;
	}

	public boolean isLobbyItem(ItemStack item) {
		return item != null && getAction(item) != null;
	}

	public boolean isLobbyWorld(World world) {
		World lobbyWorld = resolveLobbyWorld();
		return lobbyWorld != null && world != null && lobbyWorld.getUID().equals(world.getUID());
	}

	private boolean shouldApplyLobbyState(Player player) {
		if (player == null || !isLobbyWorld(player.getWorld())) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		return match == null || match.getState() == MatchState.LOBBY;
	}

	private World resolveLobbyWorld() {
		String worldName = plugin.getConfig().getString("lobby.spawn.world", "");
		if (worldName != null && !worldName.isBlank()) {
			return Bukkit.getWorld(worldName);
		}
		List<World> worlds = Bukkit.getWorlds();
		return worlds.isEmpty() ? null : worlds.get(0);
	}

	private String getAction(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return null;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		return data.get(actionKey, PersistentDataType.STRING);
	}

	private ItemStack buildMatchListItem() {
		ItemStack item = new ItemStack(Material.NETHER_STAR);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic("<gold>Phòng đấu</gold>"));
			meta.lore(mmNoItalic(List.of("<gray>Bấm để mở danh sách phòng.</gray>")));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MATCH_LIST);
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildMatchInfoItem(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.LOBBY) {
			return null;
		}
		ItemStack item = new ItemStack(Material.COMPASS);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>Thông tin phòng</yellow>"));
			meta.lore(mmNoItalic(List.of(
				"<gray>Mã phòng: " + UiColors.matchIdText(match.getId()) + "</gray>",
				"<gray>Bấm để xem chi tiết.</gray>"
			)));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_MATCH_INFO);
			item.setItemMeta(meta);
		}
		return item;
	}

	private ItemStack buildProfileItem(Player player) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		ItemMeta meta = item.getItemMeta();
		String displayName = Text.playerNameDisplay(player.getUniqueId());
		if (meta instanceof SkullMeta skullMeta) {
			skullMeta.setOwningPlayer(player);
			skullMeta.displayName(mmNoItalic("<yellow>Hồ sơ của </yellow>" + displayName));
			skullMeta.lore(mmNoItalic(List.of("<gray>Bấm để xem hồ sơ cá nhân.</gray>")));
			skullMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_PROFILE);
			item.setItemMeta(skullMeta);
		} else if (meta != null) {
			meta.displayName(mmNoItalic("<yellow>Hồ sơ của </yellow>" + displayName));
			meta.lore(mmNoItalic(List.of("<gray>Bấm để xem hồ sơ cá nhân.</gray>")));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, ACTION_PROFILE);
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
}
