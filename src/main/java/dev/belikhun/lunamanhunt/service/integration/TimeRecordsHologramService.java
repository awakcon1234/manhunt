package dev.belikhun.lunamanhunt.service.integration;

import de.oliver.fancyholograms.api.FancyHologramsPlugin;
import de.oliver.fancyholograms.api.HologramManager;
import de.oliver.fancyholograms.api.data.ItemHologramData;
import de.oliver.fancyholograms.api.data.TextHologramData;
import de.oliver.fancyholograms.api.hologram.Hologram;
import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import dev.belikhun.lunamanhunt.service.persistence.ProfileStore;
import dev.belikhun.lunamanhunt.util.MatchHistoryFormatter;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class TimeRecordsHologramService {
	private static final String ROOT = "holograms.timerecords";
	private static final String DEFAULT_NAME = "lm_timerecords";
	private static final int LAYOUT_VERSION = 2;
	private static final int MAX_LINES = 10;
	private static final String[] ROW_COLORS = {
		"#ff4b4b", "#ff7a2f", "#ffba2f", "#9ad62d", "#20c997",
		"#1bb5ff", "#2f8bff", "#6b5bff", "#9a4dff", "#d946ef"
	};
	private static final String PLACEHOLDER_LEFT = "<gray>———</gray>";
	private static final String PLACEHOLDER_RIGHT = "<gray>———</gray>";

	private final JavaPlugin plugin;
	private final ProfileStore profileStore;

	public TimeRecordsHologramService(JavaPlugin plugin, ProfileStore profileStore) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.profileStore = Objects.requireNonNull(profileStore, "profileStore");
		ensureConfigDefaults();
	}

	public String getHologramName() {
		String value = getStringOrDefault(ROOT + ".name", DEFAULT_NAME);
		return value == null || value.isBlank() ? DEFAULT_NAME : value.trim();
	}

	public boolean hasStoredLocation() {
		return plugin.getConfig().contains(ROOT + ".location.world");
	}

	public Location getStoredLocation() {
		String worldName = plugin.getConfig().getString(ROOT + ".location.world");
		if (worldName == null || worldName.isBlank()) {
			return null;
		}
		World world = Bukkit.getWorld(worldName);
		if (world == null) {
			return null;
		}
		double x = plugin.getConfig().getDouble(ROOT + ".location.x", 0.0D);
		double y = plugin.getConfig().getDouble(ROOT + ".location.y", 0.0D);
		double z = plugin.getConfig().getDouble(ROOT + ".location.z", 0.0D);
		float yaw = (float) plugin.getConfig().getDouble(ROOT + ".location.yaw", 0.0D);
		float pitch = (float) plugin.getConfig().getDouble(ROOT + ".location.pitch", 0.0D);
		return new Location(world, x, y, z, yaw, pitch);
	}

	public void setPosition(Location location) {
		if (location == null || location.getWorld() == null) {
			throw new IllegalArgumentException("location/world cannot be null");
		}
		plugin.getConfig().set(ROOT + ".location.world", location.getWorld().getName());
		plugin.getConfig().set(ROOT + ".location.x", location.getX());
		plugin.getConfig().set(ROOT + ".location.y", location.getY());
		plugin.getConfig().set(ROOT + ".location.z", location.getZ());
		plugin.getConfig().set(ROOT + ".location.yaw", location.getYaw());
		plugin.getConfig().set(ROOT + ".location.pitch", location.getPitch());
		plugin.saveConfig();
	}

	public boolean isFixedBillboard() {
		return getBooleanOrDefault(ROOT + ".style.fixed", true);
	}

	public void setFixedBillboard(boolean fixed) {
		plugin.getConfig().set(ROOT + ".style.fixed", fixed);
		plugin.saveConfig();
	}

	public String getBackgroundHex() {
		String value = getStringOrDefault(ROOT + ".style.background", "");
		return value == null ? "" : value.trim();
	}

	public void setBackgroundHex(String hexOrNone) {
		if (hexOrNone == null || hexOrNone.isBlank() || hexOrNone.equalsIgnoreCase("none")) {
			plugin.getConfig().set(ROOT + ".style.background", "");
		} else {
			plugin.getConfig().set(ROOT + ".style.background", normalizeHex(hexOrNone));
		}
		plugin.saveConfig();
	}

	public float getOpacity() {
		double value = getDoubleOrDefault(ROOT + ".style.opacity", 1.0D);
		return (float) Math.max(0.0D, Math.min(1.0D, value));
	}

	public void setOpacity(float opacity) {
		float clamped = Math.max(0.0F, Math.min(1.0F, opacity));
		plugin.getConfig().set(ROOT + ".style.opacity", clamped);
		plugin.saveConfig();
	}

	public boolean createOrRefresh(Location fallbackLocation) {
		Location location = getStoredLocation();
		if (location == null) {
			if (fallbackLocation == null || fallbackLocation.getWorld() == null) {
				return false;
			}
			setPosition(fallbackLocation);
			location = fallbackLocation;
		}

		HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
		List<RankingRow> rows = buildRows();

		upsertItemHologram(manager, componentName("icon"), componentLocation(location, "icon", 0.0D, 1.40D, 0.0D), resolveIconMaterial());
		upsertTextHologram(manager, componentName("header"), componentLocation(location, "header", 0.0D, 0.95D, 0.0D), buildHeaderLines());
		upsertTextHologram(manager, componentName("left"), componentLocation(location, "left", -0.95D, 0.58D, 0.0D), buildLeftLines(rows));
		upsertTextHologram(manager, componentName("right"), componentLocation(location, "right", 1.05D, 0.58D, 0.0D), buildRightLines(rows));
		return true;
	}

	public boolean refresh() {
		Location location = getStoredLocation();
		if (location == null) {
			return false;
		}

		HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
		if (!hasAnyBoardComponent(manager)) {
			return false;
		}
		return createOrRefresh(location);
	}

	public boolean remove() {
		HologramManager manager = FancyHologramsPlugin.get().getHologramManager();
		boolean removed = false;
		for (String suffix : List.of("icon", "header", "left", "right")) {
			Hologram hologram = manager.getHologram(componentName(suffix)).orElse(null);
			if (hologram == null) {
				continue;
			}
			manager.removeHologram(hologram);
			removed = true;
		}
		return removed;
	}

	private void upsertTextHologram(HologramManager manager, String name, Location location, List<String> lines) {
		Hologram hologram = manager.getHologram(name).orElse(null);
		if (hologram == null) {
			TextHologramData data = new TextHologramData(name, location);
			applyTextData(data, location, lines);
			manager.addHologram(manager.create(data));
			return;
		}
		if (!(hologram.getData() instanceof TextHologramData data)) {
			throw new IllegalStateException("Hologram " + name + " không phải loại text.");
		}
		applyTextData(data, location, lines);
		hologram.forceUpdate();
		hologram.queueUpdate();
	}

	private void upsertItemHologram(HologramManager manager, String name, Location location, Material material) {
		Hologram hologram = manager.getHologram(name).orElse(null);
		if (hologram == null) {
			ItemHologramData data = new ItemHologramData(name, location);
			applyItemData(data, location, material);
			manager.addHologram(manager.create(data));
			return;
		}
		if (!(hologram.getData() instanceof ItemHologramData data)) {
			throw new IllegalStateException("Hologram " + name + " không phải loại item.");
		}
		applyItemData(data, location, material);
		hologram.forceUpdate();
		hologram.queueUpdate();
	}

	private void applyTextData(TextHologramData data, Location location, List<String> lines) {
		data.setLocation(location);
		data.setText(lines);
		data.setBillboard(isFixedBillboard() ? Display.Billboard.FIXED : Display.Billboard.CENTER);
		data.setBackground(Hologram.TRANSPARENT);
	}

	private void applyItemData(ItemHologramData data, Location location, Material material) {
		data.setLocation(location);
		data.setBillboard(isFixedBillboard() ? Display.Billboard.FIXED : Display.Billboard.CENTER);
		data.setItemStack(new ItemStack(material));
	}

	private List<String> buildHeaderLines() {
		return List.of(
			"<gradient:#ff5ea8:#7cf7ff><bold>⌚ Bảng kỷ lục thời gian</bold></gradient>",
			"<gray>TOP 10 LEADERBOARD</gray>"
		);
	}

	private List<RankingRow> buildRows() {
		List<RecordEntry> entries = profileStore.loadAll().stream()
			.filter(profile -> profile.getBestTimeMillis() > 0L)
			.sorted(Comparator.comparingLong(PlayerProfile::getBestTimeMillis))
			.limit(MAX_LINES)
			.map(profile -> new RecordEntry(Text.playerNameDisplay(profile.getPlayerId()),
				MatchHistoryFormatter.formatDuration(profile.getBestTimeMillis())))
			.toList();

		List<RankingRow> rows = new ArrayList<>(MAX_LINES);
		for (int i = 0; i < MAX_LINES; i++) {
			String color = ROW_COLORS[i % ROW_COLORS.length];
			int rank = i + 1;
			if (i < entries.size()) {
				RecordEntry entry = entries.get(i);
				rows.add(new RankingRow(rank, color, entry.name(), entry.time(), true));
				continue;
			}
			rows.add(new RankingRow(rank, color, PLACEHOLDER_LEFT, PLACEHOLDER_RIGHT, false));
		}
		return rows;
	}

	private List<String> buildLeftLines(List<RankingRow> rows) {
		List<String> lines = new ArrayList<>(MAX_LINES);
		for (RankingRow row : rows) {
			String glyph = rankGlyph(row.rank());
			if (row.hasValue()) {
				lines.add("<" + row.color() + ">" + glyph + "</" + row.color() + "> <white>" + row.left() + "</white>");
				continue;
			}
			lines.add("<" + row.color() + ">" + glyph + "</" + row.color() + "> " + row.left());
		}
		return lines;
	}

	private List<String> buildRightLines(List<RankingRow> rows) {
		List<String> lines = new ArrayList<>(MAX_LINES);
		for (RankingRow row : rows) {
			if (row.hasValue()) {
				lines.add("<" + row.color() + ">" + safeMiniName(row.right()) + "</" + row.color() + ">");
				continue;
			}
			lines.add(row.right());
		}
		return lines;
	}

	private String rankGlyph(int rank) {
		return switch (rank) {
			case 1 -> "①";
			case 2 -> "②";
			case 3 -> "③";
			case 4 -> "④";
			case 5 -> "⑤";
			case 6 -> "⑥";
			case 7 -> "⑦";
			case 8 -> "⑧";
			case 9 -> "⑨";
			case 10 -> "⑩";
			default -> String.valueOf(rank);
		};
	}

	private boolean hasAnyBoardComponent(HologramManager manager) {
		for (String suffix : List.of("icon", "header", "left", "right")) {
			if (manager.getHologram(componentName(suffix)).isPresent()) {
				return true;
			}
		}
		return false;
	}

	private String componentName(String suffix) {
		return getHologramName() + "_" + suffix;
	}

	private Location offset(Location base, double dx, double dy, double dz) {
		return offsetLocal(base, dx, dy, dz);
	}

	private Location offsetLocal(Location base, double rightOffset, double upOffset, double forwardOffset) {
		Location clone = base.clone();
		double yawRad = Math.toRadians(base.getYaw());
		double sin = Math.sin(yawRad);
		double cos = Math.cos(yawRad);

		double rightX = -cos;
		double rightZ = sin;
		double forwardX = -sin;
		double forwardZ = -cos;

		double x = (rightX * rightOffset) + (forwardX * forwardOffset);
		double z = (rightZ * rightOffset) + (forwardZ * forwardOffset);
		clone.add(x, upOffset, z);
		return clone;
	}

	private Material resolveIconMaterial() {
		String raw = getStringOrDefault(ROOT + ".icon-material", "CLOCK");
		if (raw == null || raw.isBlank()) {
			return Material.CLOCK;
		}
		Material material = Material.matchMaterial(raw.trim().toUpperCase(Locale.ROOT));
		return material == null ? Material.CLOCK : material;
	}

	private Location componentLocation(Location base, String component, double defaultX, double defaultY, double defaultZ) {
		double dx = getDoubleOrDefault(ROOT + ".layout." + component + ".x", defaultX);
		double dy = getDoubleOrDefault(ROOT + ".layout." + component + ".y", defaultY);
		double dz = getDoubleOrDefault(ROOT + ".layout." + component + ".z", defaultZ);
		return offset(base, dx, dy, dz);
	}

	private void ensureConfigDefaults() {
		boolean changed = false;
		int currentLayoutVersion = plugin.getConfig().getInt(ROOT + ".layout.version", 0);
		if (currentLayoutVersion < LAYOUT_VERSION) {
			changed |= forceSet(ROOT + ".layout.version", LAYOUT_VERSION);
			changed |= forceSet(ROOT + ".layout.icon.x", 0.0D);
			changed |= forceSet(ROOT + ".layout.icon.y", 2.35D);
			changed |= forceSet(ROOT + ".layout.icon.z", 0.0D);
			changed |= forceSet(ROOT + ".layout.header.x", 0.0D);
			changed |= forceSet(ROOT + ".layout.header.y", 1.70D);
			changed |= forceSet(ROOT + ".layout.header.z", 0.0D);
			changed |= forceSet(ROOT + ".layout.left.x", -1.35D);
			changed |= forceSet(ROOT + ".layout.left.y", 0.35D);
			changed |= forceSet(ROOT + ".layout.left.z", 0.0D);
			changed |= forceSet(ROOT + ".layout.right.x", 1.35D);
			changed |= forceSet(ROOT + ".layout.right.y", 0.35D);
			changed |= forceSet(ROOT + ".layout.right.z", 0.0D);
		}

		changed |= ensureDefault(ROOT + ".name", DEFAULT_NAME);
		changed |= ensureDefault(ROOT + ".icon-material", "CLOCK");
		changed |= ensureDefault(ROOT + ".style.fixed", true);
		changed |= ensureDefault(ROOT + ".style.background", "");
		changed |= ensureDefault(ROOT + ".style.opacity", 1.0D);
		changed |= ensureDefault(ROOT + ".layout.version", LAYOUT_VERSION);
		changed |= ensureDefault(ROOT + ".layout.icon.x", 0.0D);
		changed |= ensureDefault(ROOT + ".layout.icon.y", 2.35D);
		changed |= ensureDefault(ROOT + ".layout.icon.z", 0.0D);
		changed |= ensureDefault(ROOT + ".layout.header.x", 0.0D);
		changed |= ensureDefault(ROOT + ".layout.header.y", 1.70D);
		changed |= ensureDefault(ROOT + ".layout.header.z", 0.0D);
		changed |= ensureDefault(ROOT + ".layout.left.x", -1.35D);
		changed |= ensureDefault(ROOT + ".layout.left.y", 0.35D);
		changed |= ensureDefault(ROOT + ".layout.left.z", 0.0D);
		changed |= ensureDefault(ROOT + ".layout.right.x", 1.35D);
		changed |= ensureDefault(ROOT + ".layout.right.y", 0.35D);
		changed |= ensureDefault(ROOT + ".layout.right.z", 0.0D);
		if (changed) {
			plugin.saveConfig();
		}
	}

	private boolean forceSet(String path, Object value) {
		Object current = plugin.getConfig().get(path);
		if (Objects.equals(current, value)) {
			return false;
		}
		plugin.getConfig().set(path, value);
		return true;
	}

	private boolean ensureDefault(String path, Object value) {
		if (plugin.getConfig().contains(path)) {
			return false;
		}
		plugin.getConfig().set(path, value);
		return true;
	}

	private String getStringOrDefault(String path, String fallback) {
		if (!plugin.getConfig().contains(path)) {
			plugin.getConfig().set(path, fallback);
			plugin.saveConfig();
		}
		return plugin.getConfig().getString(path, fallback);
	}

	private double getDoubleOrDefault(String path, double fallback) {
		if (!plugin.getConfig().contains(path)) {
			plugin.getConfig().set(path, fallback);
			plugin.saveConfig();
		}
		return plugin.getConfig().getDouble(path, fallback);
	}

	private boolean getBooleanOrDefault(String path, boolean fallback) {
		if (!plugin.getConfig().contains(path)) {
			plugin.getConfig().set(path, fallback);
			plugin.saveConfig();
		}
		return plugin.getConfig().getBoolean(path, fallback);
	}

	private String safeMiniName(String value) {
		if (value == null || value.isBlank()) {
			return "Người chơi";
		}
		return value.replace("<", "\\<").replace(">", "\\>");
	}

	private String normalizeHex(String input) {
		String value = input.trim();
		if (!value.startsWith("#")) {
			value = "#" + value;
		}
		if (!value.matches("#[0-9a-fA-F]{6}")) {
			throw new IllegalArgumentException("Màu phải theo dạng #RRGGBB.");
		}
		return value.toLowerCase(Locale.ROOT);
	}

	private record RecordEntry(String name, String time) {}

	private record RankingRow(int rank, String color, String left, String right, boolean hasValue) {}
}
