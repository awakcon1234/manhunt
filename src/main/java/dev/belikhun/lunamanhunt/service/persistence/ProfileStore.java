package dev.belikhun.lunamanhunt.service.persistence;

import dev.belikhun.lunamanhunt.model.match.MatchWinner;
import dev.belikhun.lunamanhunt.model.profile.MatchHistoryEntry;
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

		List<?> historyList = config.getList("matchHistory");
		if (historyList != null) {
			for (Object entryObj : historyList) {
				if (!(entryObj instanceof java.util.Map<?, ?> map)) {
					continue;
				}
				MatchHistoryEntry entry = parseHistoryEntry(map);
				if (entry != null) {
					profile.getMatchHistory().add(entry);
				}
			}
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
		config.set("matchHistory", buildHistoryList(profile));
		config.save(file);
	}

	private MatchHistoryEntry parseHistoryEntry(java.util.Map<?, ?> map) {
		MatchHistoryEntry entry = new MatchHistoryEntry();
		Object matchId = map.get("matchId");
		if (matchId instanceof String value) {
			entry.setMatchId(value);
		}
		Object winner = map.get("winner");
		if (winner instanceof String value) {
			try {
				entry.setWinner(MatchWinner.valueOf(value));
			} catch (IllegalArgumentException ignored) {
			}
		}
		Object role = map.get("role");
		if (role instanceof String value) {
			entry.setRole(value);
		}
		Object won = map.get("won");
		if (won instanceof Boolean value) {
			entry.setWon(value);
		}
		Object startedAt = map.get("startedAtMillis");
		if (startedAt instanceof Number value) {
			entry.setStartedAtMillis(value.longValue());
		}
		Object endedAt = map.get("endedAtMillis");
		if (endedAt instanceof Number value) {
			entry.setEndedAtMillis(value.longValue());
		}
		Object duration = map.get("durationMillis");
		if (duration instanceof Number value) {
			entry.setDurationMillis(value.longValue());
		}
		return entry;
	}

	private List<java.util.Map<String, Object>> buildHistoryList(PlayerProfile profile) {
		List<java.util.Map<String, Object>> list = new ArrayList<>();
		for (MatchHistoryEntry entry : profile.getMatchHistory()) {
			java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
			map.put("matchId", entry.getMatchId());
			MatchWinner winner = entry.getWinner();
			map.put("winner", winner == null ? null : winner.name());
			map.put("role", entry.getRole());
			map.put("won", entry.isWon());
			map.put("startedAtMillis", entry.getStartedAtMillis());
			map.put("endedAtMillis", entry.getEndedAtMillis());
			map.put("durationMillis", entry.getDurationMillis());
			list.add(map);
		}
		return list;
	}

	private File getProfileFile(UUID playerId) {
		return new File(profilesDir, playerId + ".yml");
	}
}
