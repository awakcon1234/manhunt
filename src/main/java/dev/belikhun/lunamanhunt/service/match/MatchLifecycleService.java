package dev.belikhun.lunamanhunt.service.match;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchSettings;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.model.match.MatchWorlds;
import dev.belikhun.lunamanhunt.model.match.SpeedrunnerSelection;
import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.util.ItemStackCodec;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class MatchLifecycleService {
	private final LobbyManager lobbyManager;
	private final MatchManager matchManager;
	private final CompassService compassService;
	private final Random random = new Random();

	public MatchLifecycleService(LobbyManager lobbyManager, MatchManager matchManager,
			CompassService compassService) {
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
		this.matchManager = Objects.requireNonNull(matchManager, "matchManager");
		this.compassService = Objects.requireNonNull(compassService, "compassService");
	}

	public void handleWorldsReady(Match match) {
		if (match == null || match.getState() != MatchState.WORLD_READY) {
			return;
		}
		MatchWorlds worlds = match.getWorlds();
		if (worlds == null) {
			markMatchFailed(match, "<red>❌</red> Không thể khởi tạo thế giới trận đấu.");
			return;
		}
		World overworld = Bukkit.getWorld(worlds.getOverworldName());
		if (overworld == null) {
			markMatchFailed(match, "<red>❌</red> Thế giới trận đấu chưa sẵn sàng.");
			return;
		}

		UUID speedrunnerId = resolveSpeedrunner(match);
		if (speedrunnerId == null) {
			markMatchFailed(match, "<red>❌</red> Không có người chơi để bắt đầu trận đấu.");
			return;
		}
		match.setSpeedrunnerId(speedrunnerId);
		assignHunters(match, speedrunnerId);

		Location spawn = overworld.getSpawnLocation();
		applyWorldBorder(overworld, spawn, match.getSettings());
		teleportPlayers(match, spawn);
		giveSpeedrunnerKit(match, speedrunnerId);

		match.setState(MatchState.RUN_PENDING);
		broadcast(match, "<yellow>ℹ</yellow> Thế giới đã sẵn sàng. Speedrunner hãy đánh thợ săn để bắt đầu.");
	}

	public boolean tryStartRun(Player attacker, Player victim) {
		if (attacker == null || victim == null) {
			return false;
		}
		Match match = lobbyManager.getMatchForPlayer(attacker.getUniqueId());
		if (match == null || match.getState() != MatchState.RUN_PENDING) {
			return false;
		}
		if (!match.getId().equals(getMatchId(victim))) {
			return false;
		}
		UUID speedrunnerId = match.getSpeedrunnerId();
		if (speedrunnerId == null || !speedrunnerId.equals(attacker.getUniqueId())) {
			return false;
		}
		if (!match.getHunters().contains(victim.getUniqueId())) {
			return false;
		}

		if (!matchManager.beginRun(match)) {
			return false;
		}
		for (UUID hunterId : Set.copyOf(match.getHunters())) {
			Player hunter = Bukkit.getPlayer(hunterId);
			if (hunter != null) {
				compassService.giveTrackingCompass(hunter, match);
			}
		}
		broadcast(match, "<green>✔</green> Trận đấu bắt đầu. Chúc may mắn!");
		return true;
	}

	private void markMatchFailed(Match match, String message) {
		match.setState(MatchState.FINISHED);
		match.setEndedAtMillis(System.currentTimeMillis());
		broadcast(match, message);
	}

	private UUID resolveSpeedrunner(Match match) {
		Set<UUID> lobbyPlayers = match.getLobbyPlayers();
		if (lobbyPlayers.isEmpty()) {
			return null;
		}
		MatchSettings settings = match.getSettings();
		UUID selected = settings.getSpeedrunnerId();
		if (settings.getSpeedrunnerSelection() == SpeedrunnerSelection.SPECIFIC) {
			if (selected != null && lobbyPlayers.contains(selected)) {
				return selected;
			}
		}
		List<UUID> candidates = new ArrayList<>(lobbyPlayers);
		return candidates.get(random.nextInt(candidates.size()));
	}

	private void assignHunters(Match match, UUID speedrunnerId) {
		match.getHunters().clear();
		match.getHunters().addAll(match.getLobbyPlayers());
		match.getHunters().remove(speedrunnerId);
	}

	private void applyWorldBorder(World world, Location center, MatchSettings settings) {
		if (world == null || center == null || settings == null) {
			return;
		}
		var border = world.getWorldBorder();
		border.setCenter(center);
		border.setSize(settings.getStartingBorder());
	}

	private void teleportPlayers(Match match, Location spawn) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				player.teleport(spawn);
			}
		}
	}

	private void giveSpeedrunnerKit(Match match, UUID speedrunnerId) {
		Player speedrunner = Bukkit.getPlayer(speedrunnerId);
		if (speedrunner == null) {
			return;
		}
		List<ItemStack> items = ItemStackCodec.decodeList(match.getSettings().getSpeedrunnerKit());
		if (items.isEmpty()) {
			return;
		}
		for (ItemStack item : items) {
			speedrunner.getInventory().addItem(item);
		}
	}

	private String getMatchId(Player player) {
		Match victimMatch = lobbyManager.getMatchForPlayer(player.getUniqueId());
		return victimMatch == null ? null : victimMatch.getId();
	}

	private void broadcast(Match match, String message) {
		for (UUID id : Set.copyOf(match.getLobbyPlayers())) {
			Player player = Bukkit.getPlayer(id);
			if (player != null) {
				Text.msg(player, message);
			}
		}
	}
}
