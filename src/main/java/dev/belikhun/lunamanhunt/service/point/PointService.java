package dev.belikhun.lunamanhunt.service.point;

import dev.belikhun.lunamanhunt.model.match.Match;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PointService {
	private static final int BACK_BASE_COST = 5;
	private static final int BACK_COST_STEP = 2;

	private final JavaPlugin plugin;
	private final Map<String, Map<UUID, Integer>> hunterPointsByMatch = new ConcurrentHashMap<>();
	private final Map<String, Map<UUID, Integer>> hunterBackUsesByMatch = new ConcurrentHashMap<>();
	private final Map<String, Map<UUID, Location>> hunterLastDeathsByMatch = new ConcurrentHashMap<>();

	public PointService(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public String formatPoints(int amount) {
		return "<#ffc012>" + amount + " 🔥</#ffc012>";
	}

	public int resolveAdvancementPoints(Match match) {
		if (match == null || plugin == null) {
			return 1;
		}
		int basePoints = cfgInt("balance.cards.advancement.basePoints", 1);
		int extraPerStep = cfgInt("balance.cards.advancement.extraPointsPerStep", 1);
		int huntersPerStep = Math.max(1, cfgInt("balance.cards.advancement.huntersPerStep", 2));
		int maxPoints = Math.max(basePoints, cfgInt("balance.cards.advancement.maxPoints", 5));

		int hunters = Math.max(0, match.getHunters().size());
		int extraSteps = Math.max(0, (hunters - 1) / huntersPerStep);
		int scaled = basePoints + (extraSteps * Math.max(0, extraPerStep));
		return Math.max(1, Math.min(maxPoints, scaled));
	}

	public int addHunterPoints(Match match, UUID hunterId, int amount) {
		if (match == null || hunterId == null || amount <= 0) {
			return 0;
		}
		Map<UUID, Integer> bucket = hunterPointsByMatch.computeIfAbsent(match.getId(), key -> new ConcurrentHashMap<>());
		int next = Math.max(0, bucket.getOrDefault(hunterId, 0) + amount);
		bucket.put(hunterId, next);
		return next;
	}

	public int getHunterPoints(Match match, UUID hunterId) {
		if (match == null || hunterId == null) {
			return 0;
		}
		Map<UUID, Integer> bucket = hunterPointsByMatch.get(match.getId());
		if (bucket == null) {
			return 0;
		}
		return Math.max(0, bucket.getOrDefault(hunterId, 0));
	}

	public void recordHunterDeath(Match match, UUID hunterId, Location location) {
		if (match == null || hunterId == null || location == null) {
			return;
		}
		Map<UUID, Location> bucket = hunterLastDeathsByMatch.computeIfAbsent(
			match.getId(), key -> new ConcurrentHashMap<>());
		bucket.put(hunterId, location.clone());
	}

	public Location getHunterLastDeath(Match match, UUID hunterId) {
		if (match == null || hunterId == null) {
			return null;
		}
		Map<UUID, Location> bucket = hunterLastDeathsByMatch.get(match.getId());
		if (bucket == null) {
			return null;
		}
		Location location = bucket.get(hunterId);
		return location == null ? null : location.clone();
	}

	public int getHunterBackCost(Match match, UUID hunterId) {
		if (match == null || hunterId == null) {
			return BACK_BASE_COST;
		}
		Map<UUID, Integer> bucket = hunterBackUsesByMatch.get(match.getId());
		int uses = bucket == null ? 0 : Math.max(0, bucket.getOrDefault(hunterId, 0));
		return BACK_BASE_COST + (uses * BACK_COST_STEP);
	}

	public BackSpendResult spendHunterBack(Match match, UUID hunterId) {
		if (match == null || hunterId == null) {
			return new BackSpendResult(false, BACK_BASE_COST, 0);
		}
		int cost = getHunterBackCost(match, hunterId);
		int points = getHunterPoints(match, hunterId);
		if (points < cost) {
			return new BackSpendResult(false, cost, points);
		}

		Map<UUID, Integer> pointsBucket = hunterPointsByMatch.computeIfAbsent(
			match.getId(), key -> new ConcurrentHashMap<>());
		int remaining = points - cost;
		pointsBucket.put(hunterId, remaining);

		Map<UUID, Integer> usesBucket = hunterBackUsesByMatch.computeIfAbsent(
			match.getId(), key -> new ConcurrentHashMap<>());
		usesBucket.put(hunterId, Math.max(0, usesBucket.getOrDefault(hunterId, 0)) + 1);

		return new BackSpendResult(true, cost, remaining);
	}

	public void clearMatchData(Match match) {
		if (match == null) {
			return;
		}
		String matchId = match.getId();
		hunterPointsByMatch.remove(matchId);
		hunterBackUsesByMatch.remove(matchId);
		hunterLastDeathsByMatch.remove(matchId);
	}

	private int cfgInt(String path, int fallback) {
		if (plugin == null || plugin.getConfig() == null) {
			return fallback;
		}
		return plugin.getConfig().getInt(path, fallback);
	}

	public record BackSpendResult(boolean success, int cost, int remainingPoints) {
	}
}
