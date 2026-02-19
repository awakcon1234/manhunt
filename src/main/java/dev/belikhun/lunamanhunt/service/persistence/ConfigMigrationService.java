package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.logging.Logger;

public class ConfigMigrationService {
	private static final String VERSION_PATH = "config-version";
	private static final String BORDER_PATH = "match.defaults.worldBorderLimit";
	private static final int CURRENT_CONFIG_VERSION = 2;
	private static final int LEGACY_BORDER_DEFAULT = 3000;

	private final JavaPlugin plugin;
	private final Logger logger;

	public ConfigMigrationService(JavaPlugin plugin) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.logger = plugin.getLogger();
	}

	public void migrateIfNeeded() {
		FileConfiguration config = plugin.getConfig();
		int configVersion = config.getInt(VERSION_PATH, 1);
		if (configVersion >= CURRENT_CONFIG_VERSION) {
			return;
		}

		boolean changed = false;
		int fromVersion = configVersion;

		if (configVersion < 2) {
			changed |= migrateToV2(config);
			configVersion = 2;
		}

		if (configVersion != CURRENT_CONFIG_VERSION) {
			configVersion = CURRENT_CONFIG_VERSION;
			changed = true;
		}

		if (config.getInt(VERSION_PATH, 1) != configVersion) {
			config.set(VERSION_PATH, configVersion);
			changed = true;
		}

		if (!changed) {
			return;
		}

		plugin.saveConfig();
		logger.info("Đã nâng cấp config.yml từ phiên bản " + fromVersion + " lên " + configVersion + ".");
	}

	private boolean migrateToV2(FileConfiguration config) {
		if (config == null) {
			return false;
		}

		Object borderRaw = config.get(BORDER_PATH);
		int defaultBorder = MatchSettings.WORLD_BORDER_LIMIT_DEFAULT;
		int minBorder = MatchSettings.WORLD_BORDER_LIMIT_MIN;
		int maxBorder = MatchSettings.WORLD_BORDER_LIMIT_MAX;

		if (borderRaw == null) {
			config.set(BORDER_PATH, defaultBorder);
			return true;
		}

		int border = parseInt(borderRaw, defaultBorder);
		if (border == LEGACY_BORDER_DEFAULT) {
			config.set(BORDER_PATH, defaultBorder);
			return true;
		}

		int clamped = Math.max(minBorder, Math.min(maxBorder, border));
		if (clamped != border) {
			config.set(BORDER_PATH, clamped);
			logger.warning("match.defaults.worldBorderLimit=" + border
				+ " nằm ngoài giới hạn mới [" + minBorder + ", " + maxBorder + "]"
				+ "; đã tự động chỉnh thành " + clamped + ".");
			return true;
		}

		return false;
	}

	private int parseInt(Object value, int fallback) {
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String text) {
			try {
				return Integer.parseInt(text.trim());
			} catch (NumberFormatException ignored) {
				return fallback;
			}
		}
		return fallback;
	}
}
