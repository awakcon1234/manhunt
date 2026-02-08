package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public class BlazeRodCard extends BaseCard {
	public BlazeRodCard() {
		super("blaze_rod", "Que Blaze", CardType.POWER_UP, CardRarity.COMMON, 1,
			"Nhận 1 que Blaze.", Material.BLAZE_ROD);
	}

	@Override
	public boolean apply(CardContext context) {
		context.giveItem(Material.BLAZE_ROD, 1);
		return true;
	}
}
