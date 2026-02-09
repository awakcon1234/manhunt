package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.service.integration.MultiverseService;
import dev.belikhun.lunamanhunt.service.worldgen.WorldGenerationService;
import dev.belikhun.lunamanhunt.util.MatchIdGenerator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.world.LoadedMultiverseWorld;
import org.mvplugins.multiverse.core.world.options.ImportWorldOptions;
import org.mvplugins.multiverse.core.world.options.LoadWorldOptions;

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
	private static final int WORLD_TOTAL = 3;
	private static final int REGISTER_DELAY_TICKS = 5;

	private final Map<String, Match> matches = new ConcurrentHashMap<>();
	private final MultiverseService multiverseService;
	private final WorldGenerationService worldGenerationService;
	private final Logger logger;
	private final JavaPlugin plugin;
	private final java.util.Map<String, WorldGenerationService.GenerationProgress> generationProgress
		= new java.util.concurrent.ConcurrentHashMap<>();
	private final java.util.Map<String, WorldGenerationService.GenerationHandle> generationHandles
		= new java.util.concurrent.ConcurrentHashMap<>();
	private MatchLifecycleService lifecycleService;

	public MatchManager(JavaPlugin plugin, MultiverseService multiverseService,
			WorldGenerationService worldGenerationService) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.multiverseService = Objects.requireNonNull(multiverseService, "multiverseService");
		this.worldGenerationService = Objects.requireNonNull(worldGenerationService, "worldGenerationService");
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
		if (match.getLobbyPlayers().size() < 2) {
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
		startWorldGeneration(match, worlds);
		return true;
	}

	private void startWorldGeneration(Match match, MatchWorlds worlds) {
		Long seed = parseSeed(match.getSettings());
		String version = match.getSettings().getWorldGenerationVersion();
		if (version == null || version.isBlank()) {
			version = Bukkit.getMinecraftVersion();
		}
		worldGenerationService.generateWorlds(
			match.getId(),
			worlds.getOverworldName(),
			worlds.getNetherName(),
			worlds.getEndName(),
			version,
			seed,
			progress -> {
				generationProgress.put(match.getId(), progress);
				if (lifecycleService != null) {
					plugin.getServer().getScheduler().runTask(plugin,
						() -> lifecycleService.updateWorldProgressActionBar(match.getId()));
				}
			}
		).whenComplete((handle, throwable) -> {
			if (throwable != null) {
				logger.log(java.util.logging.Level.WARNING,
					"Failed to generate worlds for match " + match.getId(), throwable);
				if (lifecycleService != null) {
					lifecycleService.handleWorldCreationFailed(match, worlds.getOverworldName());
				} else {
					markMatchFailed(match);
				}
				return;
			}
			if (handle != null) {
				generationHandles.put(match.getId(), handle);
				handle.beginRegistering();
			}
			plugin.getServer().getScheduler().runTask(plugin, () -> registerWorldSequence(match, worlds, 0));
		});
	}

	public WorldGenerationService.GenerationProgress getGenerationProgress(String matchId) {
		return generationProgress.get(matchId);
	}

	public WorldGenerationService.GenerationHandle getGenerationHandle(String matchId) {
		return generationHandles.get(matchId);
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

	private void registerWorldSequence(Match match, MatchWorlds worlds, int index) {
		if (match == null || worlds == null) {
			return;
		}
		if (match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
		WorldGenerationService.GenerationHandle handle = generationHandles.get(match.getId());
		if (handle != null) {
			double progress = Math.min(1.0, Math.max(0.0, index / (double) WORLD_TOTAL));
			handle.updateRegistering(progress);
		}
		String name;
		World.Environment environment;
		Runnable readyCallback;
		switch (index) {
			case 0 -> {
				name = worlds.getOverworldName();
				environment = World.Environment.NORMAL;
				readyCallback = () -> worlds.setOverworldReady(true);
			}
			case 1 -> {
				name = worlds.getNetherName();
				environment = World.Environment.NETHER;
				readyCallback = () -> worlds.setNetherReady(true);
			}
			case 2 -> {
				name = worlds.getEndName();
				environment = World.Environment.THE_END;
				readyCallback = () -> worlds.setEndReady(true);
			}
			default -> {
				return;
			}
		}
		importWorld(match, worlds, name, environment, readyCallback,
			() -> scheduleRegisterNext(match, worlds, index + 1));
	}

	private void scheduleRegisterNext(Match match, MatchWorlds worlds, int index) {
		plugin.getServer().getScheduler().runTaskLater(plugin,
			() -> registerWorldSequence(match, worlds, index), REGISTER_DELAY_TICKS);
	}

	private void importWorld(Match match, MatchWorlds worlds, String name, World.Environment environment,
			Runnable readyCallback, Runnable nextCallback) {
		multiverseService.getCoreApi().getWorldManager()
			.importWorld(ImportWorldOptions.worldName(name)
				.environment(environment)
				.doFolderCheck(true)
				.useSpawnAdjust(false))
			.onFailure(reason -> loadExistingWorld(match, worlds, name, readyCallback, nextCallback, reason))
			.onSuccess(world -> finishWorldLoad(match, worlds, world, readyCallback, nextCallback));
	}

	private void loadExistingWorld(Match match, MatchWorlds worlds, String name, Runnable readyCallback,
			Runnable nextCallback, Object reason) {
		multiverseService.getCoreApi().getWorldManager().getWorld(name)
			.peek(existingWorld -> multiverseService.getCoreApi().getWorldManager()
				.loadWorld(LoadWorldOptions.world(existingWorld).doFolderCheck(true))
				.onFailure(loadReason -> {
					logger.warning("Failed to load world " + name + ": " + loadReason);
					if (lifecycleService != null) {
						lifecycleService.handleWorldCreationFailed(match, name);
					} else {
						markMatchFailed(match);
					}
				})
				.onSuccess(loadedWorld -> finishWorldLoad(match, worlds, loadedWorld, readyCallback, nextCallback)))
			.onEmpty(() -> {
				logger.warning("Failed to import world " + name + ": " + reason);
				if (lifecycleService != null) {
					lifecycleService.handleWorldCreationFailed(match, name);
				} else {
					markMatchFailed(match);
				}
			});
	}

	private void finishWorldLoad(Match match, MatchWorlds worlds, LoadedMultiverseWorld world,
			Runnable readyCallback, Runnable nextCallback) {
		readyCallback.run();
		if (lifecycleService != null) {
			lifecycleService.handleWorldGenerated(match, world);
		}
		checkWorldsReady(match, worlds);
		if (nextCallback != null) {
			nextCallback.run();
		}
	}

	private void checkWorldsReady(Match match, MatchWorlds worlds) {
		if (match.getState() != MatchState.WORLD_CREATING) {
			return;
		}
		if (!worlds.areAllReady()) {
			return;
		}
		WorldGenerationService.GenerationHandle handle = generationHandles.get(match.getId());
		if (handle != null) {
			handle.completeRegistering();
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
		String base = "lm_" + matchId.toLowerCase();
		if ("overworld".equalsIgnoreCase(suffix)) {
			return base;
		}
		return base + "_" + suffix;
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
