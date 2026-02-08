package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.compass.CompassService;
import dev.belikhun.lunamanhunt.util.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

public class CompassListener implements Listener {
	private final CompassService compassService;

	public CompassListener(CompassService compassService) {
		this.compassService = Objects.requireNonNull(compassService, "compassService");
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (event == null || event.getItem() == null) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (!compassService.isTrackingCompass(item)) {
			return;
		}
		if (!compassService.canUseCompass(player, item)) {
			Text.msg(player, "<red>❌</red> La bàn không thuộc phòng của bạn.");
			return;
		}

		if (compassService.updateCompass(item, player)) {
			Text.msg(player, "<green>✔</green> Đã cập nhật la bàn truy tìm.");
		}
	}
}
