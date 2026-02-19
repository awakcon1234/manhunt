package dev.belikhun.lunamanhunt.service.spawn;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyActionResult;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.match.MatchManager;
import dev.belikhun.lunamanhunt.service.match.WinConditionService;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnService {
	private static final String ACTION_CONFIRM = "confirm";
	private static final String ACTION_CANCEL = "cancel";

	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final WinConditionService winConditionService;
	private final LobbyTeleportService lobbyTeleportService;
	private final NamespacedKey actionKey;
	private final Map<UUID, String> pendingConfirm = new ConcurrentHashMap<>();

	public SpawnService(JavaPlugin plugin, LobbyManager lobbyManager, MatchManager matchManager,
			WinConditionService winConditionService, LobbyTeleportService lobbyTeleportService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.winConditionService = Objects.requireNonNull(winConditionService, "winConditionService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
		this.actionKey = new NamespacedKey(plugin, "lm_spawn_action");
	}

	public void handleCommand(Player player) {
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null) {
			lobbyTeleportService.teleportToLobby(player, true);
			return;
		}
		if (requiresConfirm(match)) {
			openConfirm(player, match);
			return;
		}
		LobbyActionResult result = lobbyManager.leave(player.getUniqueId(), true);
		Text.msg(player, result.getMessage());
		lobbyTeleportService.teleportToLobby(player, true);
	}

	public boolean handleMenuClick(Player player, ItemStack item, String matchId) {
		if (player == null || item == null || matchId == null) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return false;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		String action = data.get(actionKey, PersistentDataType.STRING);
		if (action == null) {
			return false;
		}
		if (ACTION_CONFIRM.equals(action)) {
			confirm(player, matchId);
			return true;
		}
		if (ACTION_CANCEL.equals(action)) {
			cancel(player);
			return true;
		}
		return false;
	}

	public void handleMenuClose(Player player) {
		if (player == null) {
			return;
		}
		pendingConfirm.remove(player.getUniqueId());
	}

	public boolean isConfirmInventory(Inventory inventory) {
		return inventory != null && inventory.getHolder() instanceof SpawnConfirmHolder;
	}

	public String getMatchId(Inventory inventory) {
		if (inventory == null || !(inventory.getHolder() instanceof SpawnConfirmHolder holder)) {
			return null;
		}
		return holder.matchId();
	}

	private void openConfirm(Player player, Match match) {
		pendingConfirm.put(player.getUniqueId(), match.getId());
		Inventory inventory = buildConfirmInventory(match.getId());
		player.openInventory(inventory);
		Text.msg(player, "<yellow>⌛</yellow> Bạn đang ở trong trận. Xác nhận để rời khỏi trận đấu.");
	}

	private void confirm(Player player, String matchId) {
		pendingConfirm.remove(player.getUniqueId());
		Match match = matchManager.getMatch(matchId);
		if (match == null) {
			Text.msg(player, "<red>❌</red> Trận đấu không còn tồn tại.");
			lobbyTeleportService.teleportToLobby(player, true);
			return;
		}

		boolean inGame = requiresConfirm(match);
		if (inGame && isSpeedrunner(match, player.getUniqueId())) {
			boolean handled = winConditionService.handleSpeedrunnerForfeit(player);
			if (!handled) {
				lobbyManager.leave(player.getUniqueId(), false);
			}
		} else {
			lobbyManager.leave(player.getUniqueId(), !inGame);
		}

		lobbyTeleportService.teleportToLobby(player, false);
		Text.msg(player, "<green>✔</green> Đã rời khỏi trận và về sảnh.");
	}

	private void cancel(Player player) {
		pendingConfirm.remove(player.getUniqueId());
		Text.msg(player, "<yellow>ℹ</yellow> Đã hủy rời trận.");
	}

	private boolean requiresConfirm(Match match) {
		if (match == null) {
			return false;
		}
		MatchState state = match.getState();
		return state != MatchState.LOBBY && state != MatchState.FINISHED;
	}

	private boolean isSpeedrunner(Match match, UUID playerId) {
		return match != null && playerId != null && playerId.equals(match.getSpeedrunnerId());
	}


	private Inventory buildConfirmInventory(String matchId) {
		Inventory inventory = Bukkit.createInventory(new SpawnConfirmHolder(matchId), 27,
			Text.mm("<red>Xác nhận rời trận</red>"));

		ItemStack confirm = buildActionItem(Material.LIME_WOOL,
			"<green>✔</green> Rời trận",
			List.of("<gray>Bạn sẽ bỏ cuộc và về sảnh.</gray>", "<gray>Trận đấu của bạn sẽ bị tính thua.</gray>"),
			ACTION_CONFIRM);

		ItemStack cancel = buildActionItem(Material.RED_WOOL,
			"<red>❌</red> Ở lại",
			List.of("<gray>Tiếp tục ở lại trận đấu.</gray>"),
			ACTION_CANCEL);

		ItemStack info = buildInfoItem(Material.PAPER,
			"<yellow>ℹ</yellow> Lưu ý",
			List.of("<gray>Rời trận sẽ khiến bạn thua cuộc.</gray>"));

		inventory.setItem(11, confirm);
		inventory.setItem(13, info);
		inventory.setItem(15, cancel);
		return inventory;
	}

	private ItemStack buildActionItem(Material material, String name, List<String> lore, String action) {
		ItemStack item = new ItemStack(material);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.displayName(mmNoItalic(name));
			meta.lore(mmNoItalic(lore));
			meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
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

	public record SpawnConfirmHolder(String matchId) implements InventoryHolder {
		@Override
		public Inventory getInventory() {
			return null;
		}
	}
}
