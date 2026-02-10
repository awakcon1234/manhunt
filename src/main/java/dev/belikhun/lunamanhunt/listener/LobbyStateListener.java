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
		Text.msg(player, "");
		Text.msg(player, "");
		Text.msg(player, "Chào mừng đến với <b><gradient:#FF7EE3:#ED80FF:#C27EFF:#EB86FF:#8768CC>LUNA</gradient></b> <b><gradient:#EC8C8C:#FFC58B>M</gradient><gradient:#FFC58B:#FFFF9E>A</gradient><gradient:#FFFF9E:#9AFF9A>NH</gradient><gradient:#9AFF9A:#97CFF5>U</gradient><gradient:#97CFF5:#A39AFF>N</gradient><gradient:#A39AFF:#EEA0FF>T</gradient></b>!");
		Text.msg(player, "<yellow>ℹ</yellow> Dùng " + UiColors.command("/match") + " hoặc la bàn để tạo hoặc tham gia phòng.");
		Text.msg(player, "");
		Text.msg(player, "");

		player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 0.7f, 1.0f);
	}

	@EventHandler
	public void onWorldChange(PlayerChangedWorldEvent event) {
		lobbyItemService.applyLobbyStateIfEligible(event.getPlayer());
	}
}
