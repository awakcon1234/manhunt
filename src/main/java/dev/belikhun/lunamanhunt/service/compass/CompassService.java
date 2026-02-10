package dev.belikhun.lunamanhunt.service.compass;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitTask;

public class CompassService {
	private final JavaPlugin plugin;
	private final LobbyManager lobbyManager;
	private final NamespacedKey compassIdKey;
	private final NamespacedKey matchIdKey;
	private final Map<UUID, UUID> compassTargets = new ConcurrentHashMap<>();
	private final Map<UUID, org.bukkit.Location> lastCompassTargets = new ConcurrentHashMap<>();
	private final Map<UUID, Long> scrambledHunters = new ConcurrentHashMap<>();
	private final Map<UUID, List<ItemStack>> pendingRestores = new ConcurrentHashMap<>();
	private final java.util.Random random = new java.util.Random();
	private BukkitTask autoUpdateTask;

	public CompassService(JavaPlugin plugin, LobbyManager lobbyManager) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.compassIdKey = new NamespacedKey(plugin, "lm_compass_id");
		this.matchIdKey = new NamespacedKey(plugin, "lm_match_id");
	}

	public void giveTrackingCompass(Player hunter, Match match) {
		if (hunter == null || match == null) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null) {
			return;
		}

		UUID compassId = UUID.randomUUID();
		ItemStack compass = new ItemStack(org.bukkit.Material.COMPASS);
		CompassMeta meta = (CompassMeta) compass.getItemMeta();
		if (meta != null) {
			Player target = Bukkit.getPlayer(speedrunnerId);
			if (target != null) {
				meta.setLodestone(target.getLocation());
				meta.setLodestoneTracked(false);
			}
			meta.displayName(Text.mm("<yellow>La bàn truy tìm</yellow>"));
			PersistentDataContainer data = meta.getPersistentDataContainer();
			data.set(compassIdKey, PersistentDataType.STRING, compassId.toString());
			data.set(matchIdKey, PersistentDataType.STRING, match.getId());
			compass.setItemMeta(meta);
		}

		compassTargets.put(compassId, speedrunnerId);
		hunter.getInventory().addItem(compass);
		Text.msg(hunter, "<green>✔</green> Bạn đã nhận la bàn truy tìm.");
		hunter.playSound(hunter.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.7f, 1.2f);
	}

	public void startAutoUpdate() {
		stopAutoUpdate();
		autoUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllForOnlinePlayers, 0L, 20L);
	}

	public void stopAutoUpdate() {
		if (autoUpdateTask != null) {
			autoUpdateTask.cancel();
			autoUpdateTask = null;
		}
	}

	public boolean updateCompass(ItemStack item, Player holder) {
		if (item == null || holder == null) {
			return false;
		}
		if (!(item.getItemMeta() instanceof CompassMeta meta)) {
			return false;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		String id = data.get(compassIdKey, PersistentDataType.STRING);
		if (id == null || id.isBlank()) {
			return false;
		}
		UUID compassId;
		try {
			compassId = UUID.fromString(id);
		} catch (IllegalArgumentException ex) {
			return false;
		}
		UUID targetId = compassTargets.get(compassId);
		if (targetId == null) {
			return false;
		}
		Long scrambleUntil = scrambledHunters.get(holder.getUniqueId());
		if (scrambleUntil != null) {
			if (System.currentTimeMillis() <= scrambleUntil) {
				applyScrambledTarget(meta, holder, compassId);
				item.setItemMeta(meta);
				return true;
			}
			scrambledHunters.remove(holder.getUniqueId());
		}
		Player target = Bukkit.getPlayer(targetId);
		if (target == null) {
			return false;
		}
		if (!holder.getWorld().getUID().equals(target.getWorld().getUID())) {
			boolean needsUpdate = !meta.isLodestoneTracked() || meta.getLodestone() != null;
			if (needsUpdate) {
				meta.setLodestone(null);
				meta.setLodestoneTracked(true);
				item.setItemMeta(meta);
			}
			lastCompassTargets.remove(compassId);
			return true;
		}

		org.bukkit.Location targetLocation = target.getLocation();
		org.bukkit.Location lastLocation = lastCompassTargets.get(compassId);
		if (isSameBlockLocation(lastLocation, targetLocation)) {
			return true;
		}
		meta.setLodestone(targetLocation);
		meta.setLodestoneTracked(false);
		item.setItemMeta(meta);
		lastCompassTargets.put(compassId, snapshotBlockLocation(targetLocation));
		return true;
	}

	public void scrambleHunters(Match match, long durationTicks) {
		if (match == null || durationTicks <= 0) {
			return;
		}
		long until = System.currentTimeMillis() + (durationTicks * 50L);
		java.util.Set<UUID> targets = match.getHunters();
		if (targets.isEmpty()) {
			targets = match.getLobbyPlayers();
		}
		java.util.Set<UUID> targetIds = new java.util.HashSet<>(targets);
		for (UUID id : targetIds) {
			scrambledHunters.put(id, until);
		}
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (UUID id : targetIds) {
				Long value = scrambledHunters.get(id);
				if (value != null && value <= System.currentTimeMillis()) {
					scrambledHunters.remove(id);
				}
			}
		}, durationTicks + 1);
	}

	public void clearScramble(Match match) {
		if (match == null) {
			return;
		}
		Set<UUID> targets = match.getHunters();
		if (targets.isEmpty()) {
			targets = match.getLobbyPlayers();
		}
		for (UUID id : Set.copyOf(targets)) {
			scrambledHunters.remove(id);
		}
	}

	private void applyScrambledTarget(CompassMeta meta, Player holder, UUID compassId) {
		if (meta == null || holder == null) {
			return;
		}
		int offsetX = random.nextInt(301) - 150;
		int offsetZ = random.nextInt(301) - 150;
		org.bukkit.World world = holder.getWorld();
		int baseX = holder.getLocation().getBlockX() + offsetX;
		int baseZ = holder.getLocation().getBlockZ() + offsetZ;
		int baseY = world.getHighestBlockYAt(baseX, baseZ) + 1;
		org.bukkit.Location scrambled = new org.bukkit.Location(world, baseX, baseY, baseZ);
		meta.setLodestone(scrambled);
		meta.setLodestoneTracked(false);
		if (compassId != null) {
			lastCompassTargets.put(compassId, snapshotBlockLocation(scrambled));
		}
	}

	private boolean isSameBlockLocation(org.bukkit.Location left, org.bukkit.Location right) {
		if (left == null || right == null) {
			return false;
		}
		if (!left.getWorld().getUID().equals(right.getWorld().getUID())) {
			return false;
		}
		return left.getBlockX() == right.getBlockX()
			&& left.getBlockY() == right.getBlockY()
			&& left.getBlockZ() == right.getBlockZ();
	}

	private org.bukkit.Location snapshotBlockLocation(org.bukkit.Location location) {
		if (location == null) {
			return null;
		}
		return new org.bukkit.Location(
			location.getWorld(),
			location.getBlockX(),
			location.getBlockY(),
			location.getBlockZ()
		);
	}

	public void updateAllForOnlinePlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			ItemStack[] contents = player.getInventory().getContents();
			if (contents == null) {
				continue;
			}
			for (ItemStack item : contents) {
				if (!isTrackingCompass(item) || !canUseCompass(player, item)) {
					continue;
				}
				updateCompass(item, player);
			}
		}
	}

	public boolean hasTrackingCompass(Player player) {
		if (player == null) {
			return false;
		}
		ItemStack[] contents = player.getInventory().getContents();
		if (contents == null) {
			return false;
		}
		for (ItemStack item : contents) {
			if (isTrackingCompass(item) && canUseCompass(player, item)) {
				return true;
			}
		}
		return false;
	}

	public void stashTrackingCompasses(Player player, List<ItemStack> drops) {
		if (player == null || drops == null || drops.isEmpty()) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null && speedrunnerId.equals(player.getUniqueId())) {
			return;
		}
		List<ItemStack> saved = new ArrayList<>();
		java.util.Iterator<ItemStack> iterator = drops.iterator();
		while (iterator.hasNext()) {
			ItemStack item = iterator.next();
			if (!isTrackingCompass(item) || !canUseCompass(player, item)) {
				continue;
			}
			saved.add(item.clone());
			iterator.remove();
		}
		if (!saved.isEmpty()) {
			pendingRestores.put(player.getUniqueId(), saved);
		}
	}

	public void restoreTrackingCompasses(Player player) {
		if (player == null) {
			return;
		}
		List<ItemStack> items = pendingRestores.remove(player.getUniqueId());
		if (items == null || items.isEmpty()) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match == null || match.getState() != MatchState.RUNNING) {
			return;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId != null && speedrunnerId.equals(player.getUniqueId())) {
			return;
		}
		for (ItemStack item : items) {
			player.getInventory().addItem(item);
		}
	}

	public boolean isTrackingCompass(ItemStack item) {
		if (item == null || !(item.getItemMeta() instanceof CompassMeta meta)) {
			return false;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		return data.has(compassIdKey, PersistentDataType.STRING);
	}

	public boolean canUseCompass(Player player, ItemStack item) {
		if (player == null || item == null) {
			return false;
		}
		if (!(item.getItemMeta() instanceof CompassMeta meta)) {
			return false;
		}
		PersistentDataContainer data = meta.getPersistentDataContainer();
		String matchId = data.get(matchIdKey, PersistentDataType.STRING);
		if (matchId == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		return match != null && match.getId().equals(matchId);
	}
}
