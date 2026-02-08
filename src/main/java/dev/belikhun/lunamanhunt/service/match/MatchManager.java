package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.util.MatchIdGenerator;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.world.options.CreateWorldOptions;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class MatchManager {
	private static final int MATCH_ID_LENGTH = 7;
	private static final int MAX_ID_ATTEMPTS = 1000;

	private final Map<String, Match> matches = new ConcurrentHashMap<>();
	private final MultiverseService multiverseService;
	private final Logger logger;
	private MatchLifecycleService lifecycleService;

	public MatchManager(JavaPlugin plugin, MultiverseService multiverseService) {
		this.multiverseService = Objects.requireNonNull(multiverseService, "multiverseService");
		this.logger = plugin.getLogger();
	}

	public void setLifecycleService(MatchLifecycleService lifecycleService) {
		this.lifecycleService = lifecycleService;
	}

	public Match createMatch(UUID ownerId, MatchSettings settings) {
		String id = generateUniqueId();
		Match match = new Match(id, ownerId, settings);
		match.getLobbyPlayers().add(ownerId);
		matches.put(id, match);
		return match;
	}

	public Match getMatch(String id) {
		return matches.get(id);
	}

	public Collection<Match> getMatches() {
		return Collections.unmodifiableCollection(matches.values());
	}

	public boolean removeMatch(String id) {
		return matches.remove(id) != null;
	}

	public boolean startMatch(Match match) {
		if (match == null) {
			return false;
		}
		if (match.getState() != MatchState.LOBBY) {
			return false;
		}

		match.setState(MatchState.WORLD_CREATING);
		MatchWorlds worlds = new MatchWorlds();
		worlds.setOverworldName(buildWorldName(match.getId(), "overworld"));
		worlds.setNetherName(buildWorldName(match.getId(), "nether"));
		worlds.setEndName(buildWorldName(match.getId(), "end"));
		match.setWorlds(worlds);
		if (lifecycleService != null) {
			lifecycleService.handleWorldsCreating(match);
		}

		createWorld(match, worlds, worlds.getOverworldName(), World.Environment.NORMAL, () -> worlds.setOverworldReady(true));
		createWorld(match, worlds, worlds.getNetherName(), World.Environment.NETHER, () -> worlds.setNetherReady(true));
		createWorld(match, worlds, worlds.getEndName(), World.Environment.THE_END, () -> worlds.setEndReady(true));
		return true;
	}

	public boolean beginRun(Match match) {
		if (match == null || match.getState() != MatchState.RUN_PENDING) {
			return false;
		}
		match.setState(MatchState.RUNNING);
		match.setStartedAtMillis(System.currentTimeMillis());
		return true;
	}

	public void endMatch(Match match) {
		if (match == null) {
			return;
		}
		match.setState(MatchState.ENDING);
		match.setEndedAtMillis(System.currentTimeMillis());
		match.setState(MatchState.FINISHED);
	}

	private void createWorld(Match match, MatchWorlds worlds, String name, World.Environment environment,
			Runnable readyCallback) {
		CreateWorldOptions options = CreateWorldOptions.worldName(name)
			.environment(environment)
			.generateStructures(true);

		Long seed = parseSeed(match.getSettings());
		if (seed != null) {
			options = options.seed(seed);
		}

		multiverseService.getCoreApi().getWorldManager()
			.createWorld(options)
			.onFailure(reason -> {
				logger.warning("Failed to create world " + name + ": " + reason);
				markMatchFailed(match);
			})
			.onSuccess(world -> {
				readyCallback.run();
				if (lifecycleService != null) {
					lifecycleService.handleWorldGenerated(match, world);
				}
				checkWorldsReady(match, worlds);
			});
	}

	private void checkWorldsReady(Match match, MatchWorlds worlds) {
		if (match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
		if (!worlds.areAllReady()) {
			return;
		}
		match.setState(MatchState.WORLD_READY);
		if (lifecycleService != null) {
			lifecycleService.handleWorldsReady(match);
		} else {
			match.setState(MatchState.RUN_PENDING);
		}
	}

	private void markMatchFailed(Match match) {
		match.setState(MatchState.FINISHED);
		match.setEndedAtMillis(System.currentTimeMillis());
	}

	private String buildWorldName(String matchId, String suffix) {
		return "lm_" + matchId.toLowerCase() + "_" + suffix;
	}

	private Long parseSeed(MatchSettings settings) {
		String seed = settings.getWorldSeed();
		if (seed == null || seed.isBlank()) {
			return null;
		}
		try {
			return Long.parseLong(seed.trim());
		} catch (NumberFormatException ex) {
			return (long) seed.trim().hashCode();
		}
	}

	private String generateUniqueId() {
		for (int i = 0; i < MAX_ID_ATTEMPTS; i++) {
			String id = MatchIdGenerator.generateId(MATCH_ID_LENGTH);
			if (!matches.containsKey(id)) {
				return id;
			}
		}
		throw new IllegalStateException("Unable to generate unique match id");
	}
}
