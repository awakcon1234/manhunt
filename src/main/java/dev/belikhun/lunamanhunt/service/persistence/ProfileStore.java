package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.model.profile.PlayerProfile;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProfileStore {
    private final File profilesDir;

    public ProfileStore(File profilesDir) {
        this.profilesDir = profilesDir;
        if (!profilesDir.exists() && !profilesDir.mkdirs()) {
            throw new IllegalStateException("Unable to create profiles folder");
        }
    }

    public PlayerProfile load(UUID playerId) {
        File file = getProfileFile(playerId);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        PlayerProfile profile = new PlayerProfile(playerId);
        profile.setBestTimeMillis(config.getLong("bestTimeMillis", 0L));
        profile.setTotalWins(config.getInt("totalWins", 0));
        profile.setTotalLosses(config.getInt("totalLosses", 0));

        List<Long> runTimes = config.getLongList("runTimesMillis");
        if (!runTimes.isEmpty()) {
            profile.getRunTimesMillis().addAll(runTimes);
        }

        return profile;
    }

    public List<PlayerProfile> loadAll() {
        List<PlayerProfile> profiles = new ArrayList<>();
        File[] files = profilesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            return profiles;
        }

        for (File file : files) {
            String name = file.getName();
            String uuidPart = name.substring(0, name.length() - 4);
            try {
                UUID playerId = UUID.fromString(uuidPart);
                profiles.add(load(playerId));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return profiles;
    }

    public void save(PlayerProfile profile) throws IOException {
        File file = getProfileFile(profile.getPlayerId());
        YamlConfiguration config = new YamlConfiguration();
        config.set("bestTimeMillis", profile.getBestTimeMillis());
        config.set("totalWins", profile.getTotalWins());
        config.set("totalLosses", profile.getTotalLosses());
        config.set("runTimesMillis", profile.getRunTimesMillis());
        config.save(file);
    }

    private File getProfileFile(UUID playerId) {
        return new File(profilesDir, playerId + ".yml");
    }
}
