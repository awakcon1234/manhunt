package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;

public class CurseInterdimensionalSicknessCard extends BaseCard {
	public CurseInterdimensionalSicknessCard() {
		super("interdimensional_sickness", "Say không gian", CardType.CURSE, CardRarity.RARE, 2,
			"Mỗi lần đi qua cổng, " + UiColors.roleHunterLower() + " sẽ bị hiệu ứng xấu trong thời gian ngắn.",
			Material.CRYING_OBSIDIAN, 120);
	}

	@Override
	public boolean apply(CardContext context) {
		return context != null && context.getMatch() != null;
	}
}
