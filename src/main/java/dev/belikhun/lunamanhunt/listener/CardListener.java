package dev.belikhun.lunamanhunt.listener;

import dev.belikhun.lunamanhunt.service.card.CardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

import java.util.Objects;

public class CardListener implements Listener {
	private final CardService cardService;

	public CardListener(CardService cardService) {
		this.cardService = Objects.requireNonNull(cardService, "cardService");
	}

	@EventHandler
	public void onAdvancement(PlayerAdvancementDoneEvent event) {
		Player player = event.getPlayer();
		if (player == null) {
			return;
		}

		String key = event.getAdvancement().getKey().getKey();
		if (key != null && key.startsWith("recipes/")) {
			return;
		}

		cardService.handleSpeedrunnerAdvancement(player);
	}
}
