package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public class SpawnistCard extends BaseCard {
	public SpawnistCard() {
		super("spawnist", "Cơ hội thứ hai", CardType.POWER_UP, CardRarity.EPIC, 3,
			"Nhận thêm một mạng kiểu Totem khi máu về 0.", Material.TOTEM_OF_UNDYING);
	}

	@Override
	public boolean apply(CardContext context) {
		if (context == null || context.getMatch() == null || context.getCardService() == null) {
			return false;
		}
		context.getCardService().addSpawnistCharge(context.getMatch(), 1);
		return true;
	}
}
