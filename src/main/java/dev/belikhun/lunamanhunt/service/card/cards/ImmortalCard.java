package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public class ImmortalCard extends BaseCard {
	public ImmortalCard() {
		super("immortal", "Bất tử", CardType.EFFECT, CardRarity.LEGENDARY, 4,
			"Nhận trạng thái bất tử trong 30 giây.", Material.NETHER_STAR, 30);
	}

	@Override
	public boolean apply(CardContext context) {
		return context != null && context.getMatch() != null;
	}
}
