package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MatchSettingsService {
	private final JavaPlugin plugin;

	public MatchSettingsService(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
	}

	public MatchSettings createDefaults() {
		MatchSettings settings = new MatchSettings();
		FileConfiguration config = plugin.getConfig();

		String selectionValue = config.getString("match.defaults.speedrunnerSelection",
			settings.getSpeedrunnerSelection().name());
		settings.setSpeedrunnerSelection(parseSpeedrunnerSelection(selectionValue, settings.getSpeedrunnerSelection()));

		String worldSeed = normalizeSeed(config.getString("match.defaults.worldSeed"));
		settings.setWorldSeed(worldSeed);

		int borderLimit = config.getInt("match.defaults.worldBorderLimit", settings.getWorldBorderLimit());
		settings.setWorldBorderLimit(borderLimit > 0 ? borderLimit : settings.getWorldBorderLimit());

		int startingBorder = config.getInt("match.defaults.startingBorder", settings.getStartingBorder());
		settings.setStartingBorder(startingBorder > 0 ? startingBorder : settings.getStartingBorder());

		String visibilityValue = config.getString("match.defaults.visibility", settings.getVisibility().name());
		settings.setVisibility(parseVisibility(visibilityValue, settings.getVisibility()));

		List<String> kit = config.getStringList("match.defaults.speedrunnerKit");
		if (!kit.isEmpty()) {
			settings.setSpeedrunnerKit(new ArrayList<>(kit));
		}

		List<String> enabledCards = config.getStringList("match.defaults.enabledCards");
		if (!enabledCards.isEmpty()) {
			settings.setEnabledCardIds(new ArrayList<>(enabledCards));
		}

		return settings;
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
}
