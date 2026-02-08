package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.card.CardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.Objects;

public class CardEffectListener implements Listener {
	private final CardService cardService;

	public CardEffectListener(CardService cardService) {
		this.cardService = Objects.requireNonNull(cardService, "cardService");
	}

	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		if (event == null) {
			return;
		}
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}
		cardService.reapplyActiveCurseEffects(player);
	}
}
