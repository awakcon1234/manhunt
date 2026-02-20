package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;

public class StunCard extends BaseCard {
	public StunCard() {
		super("stun", "Choáng", CardType.POWER_UP, CardRarity.RARE, 2,
			"Đòn đánh của Speedrunner có 10% làm choáng " + UiColors.roleHunterLower() + " trong 5 giây.",
			Material.TURTLE_HELMET, 60);
	}

	@Override
	public boolean apply(CardContext context) {
		return context != null && context.getMatch() != null;
	}
}
