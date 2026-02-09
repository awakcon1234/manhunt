package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.model.match.WorldDifficultySetting;
import dev.belikhun.lunamanhunt.model.match.WorldTimeSetting;
import dev.belikhun.lunamanhunt.model.match.WorldWeatherSetting;
import dev.belikhun.lunamanhunt.service.kit.SpeedrunnerKitService;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class MatchSettingsService {
	private static final Set<String> SUPPORTED_WORLD_GEN_VERSIONS = Set.of(
		"1.7.10",
		"1.8.9",
		"1.11.2",
		"1.12",
		"1.15.2",
		"1.16.1",
		"1.21.11"
	);

	private final JavaPlugin plugin;
	private final SpeedrunnerKitService kitService;

	public MatchSettingsService(JavaPlugin plugin, SpeedrunnerKitService kitService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.kitService = kitService;
	}

	public MatchSettings createDefaults() {
		MatchSettings settings = new MatchSettings();
		FileConfiguration config = plugin.getConfig();

		String selectionValue = config.getString("match.defaults.speedrunnerSelection",
			settings.getSpeedrunnerSelection().name());
		settings.setSpeedrunnerSelection(parseSpeedrunnerSelection(selectionValue, settings.getSpeedrunnerSelection()));

		String worldSeed = normalizeSeed(config.getString("match.defaults.worldSeed"));
		settings.setWorldSeed(worldSeed);

		String genVersion = resolveWorldGenerationVersion(
			config.getString("match.defaults.worldGenerationVersion"));
		settings.setWorldGenerationVersion(genVersion);

		String worldTimeValue = config.getString("match.defaults.worldTime", settings.getWorldTime().name());
		settings.setWorldTime(parseWorldTime(worldTimeValue, settings.getWorldTime()));

		String worldWeatherValue = config.getString("match.defaults.worldWeather", settings.getWorldWeather().name());
		settings.setWorldWeather(parseWorldWeather(worldWeatherValue, settings.getWorldWeather()));

		String worldDifficultyValue = config.getString("match.defaults.worldDifficulty",
			settings.getWorldDifficulty().name());
		settings.setWorldDifficulty(parseWorldDifficulty(worldDifficultyValue, settings.getWorldDifficulty()));

		Object keepInventoryValue = config.get("match.defaults.keepInventory");
		settings.setKeepInventory(parseKeepInventory(keepInventoryValue, settings.isKeepInventory()));

		int borderLimit = config.getInt("match.defaults.worldBorderLimit", settings.getWorldBorderLimit());
		settings.setWorldBorderLimit(borderLimit > 0 ? borderLimit : settings.getWorldBorderLimit());

		int startingBorder = config.getInt("match.defaults.startingBorder", settings.getStartingBorder());
		settings.setStartingBorder(startingBorder > 0 ? startingBorder : settings.getStartingBorder());

		String visibilityValue = config.getString("match.defaults.visibility", settings.getVisibility().name());
		settings.setVisibility(parseVisibility(visibilityValue, settings.getVisibility()));

		String kitId = config.getString("match.defaults.speedrunnerKitId");
		List<String> kit = config.getStringList("match.defaults.speedrunnerKit");
		if (kitId != null && !kitId.isBlank()) {
			settings.setSpeedrunnerKitId(kitId.trim());
			if (kitService != null) {
				kitService.applyKit(settings, kitId.trim());
			}
		} else if (!kit.isEmpty()) {
			settings.setSpeedrunnerKit(new ArrayList<>(kit));
		} else if (kitService != null) {
			kitService.applyKit(settings, kitService.getDefaultKitId());
		}

		List<String> enabledCards = config.getStringList("match.defaults.enabledCards");
		if (!enabledCards.isEmpty()) {
			settings.setEnabledCardIds(new ArrayList<>(enabledCards));
		}

		return settings;
	}

	public String resolveWorldGenerationVersion(String value) {
		String resolved = normalizeWorldGenerationVersion(value);
		if (SUPPORTED_WORLD_GEN_VERSIONS.contains(resolved)) {
			return resolved;
		}
		String fallback = normalizeWorldGenerationVersion(Bukkit.getMinecraftVersion());
		if (SUPPORTED_WORLD_GEN_VERSIONS.contains(fallback)) {
			plugin.getLogger().warning("Unsupported world generation version '" + resolved
				+ "'. Falling back to server version '" + fallback + "'.");
			return fallback;
		}
		String defaultVersion = "1.21.11";
		plugin.getLogger().warning("Unsupported world generation version '" + resolved
			+ "'. Falling back to default '" + defaultVersion + "'.");
		return defaultVersion;
	}

	public void applyKit(MatchSettings settings, String kitId) {
		if (settings == null) {
			return;
		}
		if (kitService != null) {
			kitService.applyKit(settings, kitId);
			return;
		}
		settings.setSpeedrunnerKitId(kitId);
	}

	public static SpeedrunnerSelection parseSpeedrunnerSelection(String value, SpeedrunnerSelection fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return SpeedrunnerSelection.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	public static MatchVisibility parseVisibility(String value, MatchVisibility fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return MatchVisibility.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	public static String normalizeSeed(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public static String normalizeWorldGenerationVersion(String value) {
		if (value == null) {
			return "";
		}
		String trimmed = value.trim();
		if (trimmed.isEmpty() || "current".equalsIgnoreCase(trimmed)) {
			return Bukkit.getMinecraftVersion();
		}
		return trimmed;
	}

	public static Set<String> getSupportedWorldGenVersions() {
		return new LinkedHashSet<>(SUPPORTED_WORLD_GEN_VERSIONS);
	}

	public static WorldTimeSetting parseWorldTime(String value, WorldTimeSetting fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return WorldTimeSetting.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	public static WorldWeatherSetting parseWorldWeather(String value, WorldWeatherSetting fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		try {
			return WorldWeatherSetting.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	public static WorldDifficultySetting parseWorldDifficulty(String value, WorldDifficultySetting fallback) {
		if (value == null || value.isBlank()) {
			return fallback;
		}
		String normalized = value.trim().toUpperCase(Locale.ROOT);
		if ("NORMAL".equals(normalized) || "MEDIUM".equals(normalized)) {
			return WorldDifficultySetting.MEDIUM;
		}
		try {
			return WorldDifficultySetting.valueOf(normalized);
		} catch (IllegalArgumentException ex) {
			return fallback;
		}
	}

	public static boolean parseKeepInventory(Object value, boolean fallback) {
		if (value == null) {
			return fallback;
		}
		if (value instanceof Boolean boolValue) {
			return boolValue;
		}
		if (value instanceof Number numberValue) {
			return numberValue.intValue() != 0;
		}
		String text = value.toString().trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			return fallback;
		}
		return switch (text) {
			case "on", "true", "yes", "1" -> true;
			case "off", "false", "no", "0" -> false;
			default -> fallback;
		};
	}
}
