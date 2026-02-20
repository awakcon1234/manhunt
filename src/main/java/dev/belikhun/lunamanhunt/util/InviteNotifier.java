package dev.belikhun.lunamanhunt.util;

import dev.belikhun.lunamanhunt.model.match.Match;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class InviteNotifier {
	private InviteNotifier() {
	}

	public static void notifyMatchInvite(Player target, Match match) {
		if (target == null || match == null) {
			return;
		}
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<green>✔</green> Bạn vừa nhận lời mời vào phòng " + UiColors.matchIdText(match.getId()) + "."));
		Text.send(target, Text.mm(Text.prefixMiniMessage()
			+ "<gray>Chọn: </gray>"
			+ "<green><click:run_command:'/m accept'><hover:show_text:'<gray>Bấm để chấp nhận</gray>'><b>[CHẤP NHẬN]</b></hover></click></green> "
			+ "<red><click:run_command:'/m decline'><hover:show_text:'<gray>Bấm để từ chối</gray>'><b>[TỪ CHỐI]</b></hover></click></red> "
			+ "<gray>(</gray>" + UiColors.command("/m accept") + "<gray> | </gray>"
			+ UiColors.command("/m decline") + "<gray>)</gray>"));
		target.playSound(target.getLocation(), Sound.UI_TOAST_IN, 1.2f, 0.8f);
		target.playSound(target.getLocation(), Sound.BLOCK_BELL_USE, 1.4f, 1.0f);
	}
}
