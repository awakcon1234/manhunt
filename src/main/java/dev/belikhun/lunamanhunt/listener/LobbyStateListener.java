package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.spawn.LobbyItemService;
import dev.belikhun.lunamanhunt.service.spawn.LobbyTeleportService;
import dev.belikhun.lunamanhunt.util.Text;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public class LobbyStateListener implements Listener {
	private final LobbyItemService lobbyItemService;
	private final LobbyTeleportService lobbyTeleportService;

	public LobbyStateListener(LobbyItemService lobbyItemService, LobbyTeleportService lobbyTeleportService) {
		this.lobbyItemService = Objects.requireNonNull(lobbyItemService, "lobbyItemService");
		this.lobbyTeleportService = Objects.requireNonNull(lobbyTeleportService, "lobbyTeleportService");
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		var player = event.getPlayer();
		player.getInventory().clear();
		player.getInventory().setArmorContents(null);
		player.getInventory().setItemInOffHand(null);
		lobbyTeleportService.teleportToLobby(player, false);
		Text.msg(player, "<yellow>ℹ</yellow> Chào mừng đến với <white>LunaManhunt</white>!");
		Text.msg(player, "<yellow>ℹ</yellow> Dùng " + UiColors.command("/match") + " để tạo hoặc tham gia phòng.");
		player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.0f);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		lobbyItemService.applyLobbyStateIfEligible(event.getPlayer());
	}
}
