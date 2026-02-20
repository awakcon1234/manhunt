package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.model.match.Match;
import dev.belikhun.lunamanhunt.model.match.MatchState;
import dev.belikhun.lunamanhunt.service.lobby.LobbyManager;
import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class LobbyStateListener implements Listener {
	private final JavaPlugin plugin;
	private final LobbyItemService lobbyItemService;
	private final LobbyTeleportService lobbyTeleportService;
	private final LobbyManager lobbyManager;

	public LobbyStateListener(JavaPlugin plugin, LobbyItemService lobbyItemService, LobbyTeleportService lobbyTeleportService,
			LobbyManager lobbyManager) {
		this.plugin = Objects.requireNonNull(plugin, "plugin");
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
		this.lobbyManager = Objects.requireNonNull(lobbyManager, "lobbyManager");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		var player = event.getPlayer();
		Match match = lobbyManager.getMatchForPlayer(player.getUniqueId());
		if (match != null && match.getState() != MatchState.LOBBY) {
			return;
		}
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.getInventory().setItemInOffHand(null);
		lobbyTeleportService.teleportToLobby(player, false);
		Text.msg(player, "");
		Text.msg(player, "");
		Text.msg(player, "Chào mừng đến với <b><gradient:#FF7EE3:#ED80FF:#C27EFF:#EB86FF:#8768CC>LUNA</gradient></b> <b><gradient:#EC8C8C:#FFC58B>M</gradient><gradient:#FFC58B:#FFFF9E>A</gradient><gradient:#FFFF9E:#9AFF9A>NH</gradient><gradient:#9AFF9A:#97CFF5>U</gradient><gradient:#97CFF5:#A39AFF>N</gradient><gradient:#A39AFF:#EEA0FF>T</gradient></b>!");
		Text.msg(player, "<yellow>ℹ</yellow> Dùng " + UiColors.command("/match") + " hoặc la bàn để tạo hoặc tham gia phòng.");
		Text.msg(player, "");
		Text.msg(player, "");

		player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.0f);
		Bukkit.getScheduler().runTaskLater(plugin, () -> lobbyItemService.applyLobbyStateIfEligible(player), 1L);
		Bukkit.getScheduler().runTaskLater(plugin, () -> lobbyItemService.applyLobbyStateIfEligible(player), 20L);
	}

	@EventHandler
	public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
		var connection = event.getConnection();
		if (connection == null) {
			return;
		}
		var profile = connection.getProfile();
		if (profile == null || profile.getId() == null) {
			return;
		}
		Match match = lobbyManager.getMatchForPlayer(profile.getId());
		if (match != null && match.getState() != MatchState.LOBBY) {
			return;
		}
		var spawn = lobbyTeleportService.resolveLobbySpawn(null);
		if (spawn != null) {
			event.setSpawnLocation(spawn);
		}
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		lobbyItemService.applyLobbyStateIfEligible(event.getPlayer());
	}
}
