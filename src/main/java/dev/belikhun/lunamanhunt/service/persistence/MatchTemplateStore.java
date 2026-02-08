package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchTemplate;
import dev.belikhun.lunamanhunt.model.match.MatchVisibility;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.service.match.MatchSettingsService;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MatchTemplateStore {
    private final File file;
    private final MatchSettingsService matchSettingsService;

    public MatchTemplateStore(File file, MatchSettingsService matchSettingsService) {
        this.file = file;
        this.matchSettingsService = matchSettingsService;
    }

    public MatchTemplate load(String name) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("templates." + name);
        if (section == null) {
            return null;
        }

        MatchSettings settings = readSettings(section);
        return new MatchTemplate(name, settings);
    }

    public Map<String, MatchTemplate> loadAll() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("templates");
        Map<String, MatchTemplate> templates = new HashMap<>();
        if (root == null) {
            return templates;
        }

        for (String key : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(key);
            if (section == null) {
                continue;
            }
            MatchSettings settings = readSettings(section);
            templates.put(key, new MatchTemplate(key, settings));
        }

        return templates;
    }

    public void save(MatchTemplate template) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.createSection("templates." + template.getName());
        writeSettings(section, template.getSettings());
        config.save(file);
    }

    public void delete(String name) throws IOException {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("templates." + name, null);
        config.save(file);
    }

    private MatchSettings readSettings(ConfigurationSection section) {
        MatchSettings settings = matchSettingsService != null
            ? matchSettingsService.createDefaults()
            : new MatchSettings();

        if (section.isSet("speedrunnerSelection")) {
            String selection = section.getString("speedrunnerSelection", SpeedrunnerSelection.RANDOM.name());
            settings.setSpeedrunnerSelection(MatchSettingsService.parseSpeedrunnerSelection(
                selection, settings.getSpeedrunnerSelection()));
        }

        String speedrunnerId = section.getString("speedrunnerId");
        if (speedrunnerId != null && !speedrunnerId.isEmpty()) {
            settings.setSpeedrunnerId(UUID.fromString(speedrunnerId));
        }

        if (section.isSet("worldSeed")) {
            settings.setWorldSeed(MatchSettingsService.normalizeSeed(section.getString("worldSeed")));
        }
        if (section.isSet("worldBorderLimit")) {
            settings.setWorldBorderLimit(section.getInt("worldBorderLimit", settings.getWorldBorderLimit()));
        }
        if (section.isSet("startingBorder")) {
            settings.setStartingBorder(section.getInt("startingBorder", settings.getStartingBorder()));
        }
        if (section.contains("speedrunnerKit")) {
            List<String> kit = section.getStringList("speedrunnerKit");
            settings.setSpeedrunnerKit(new ArrayList<>(kit));
        }
        if (section.contains("enabledCards")) {
            List<String> enabledCards = section.getStringList("enabledCards");
            settings.setEnabledCardIds(new ArrayList<>(enabledCards));
        }
        if (section.isSet("visibility")) {
            String visibility = section.getString("visibility", MatchVisibility.PUBLIC.name());
            settings.setVisibility(MatchSettingsService.parseVisibility(visibility, settings.getVisibility()));
        }

        return settings;
    }

    private void writeSettings(ConfigurationSection section, MatchSettings settings) {
        section.set("speedrunnerSelection", settings.getSpeedrunnerSelection().name());
        section.set("speedrunnerId", settings.getSpeedrunnerId() == null ? null : settings.getSpeedrunnerId().toString());
        section.set("worldSeed", settings.getWorldSeed());
        section.set("worldBorderLimit", settings.getWorldBorderLimit());
        section.set("startingBorder", settings.getStartingBorder());
        section.set("speedrunnerKit", settings.getSpeedrunnerKit());
        section.set("enabledCards", settings.getEnabledCardIds());
        section.set("visibility", settings.getVisibility().name());
    }
}
