package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import dev.belikhun.lunamanhunt.util.UiColors;
import org.bukkit.Material;

public class CurseVeganCard extends BaseCard {
	public CurseVeganCard() {
		super("vegan", "Thuần chay", CardType.CURSE, CardRarity.RARE, 2,
			UiColors.roleHunter() + " chỉ được ăn đồ chay trong 5 phút.", Material.BEETROOT_SOUP, 300);
	}

	@Override
	public boolean apply(CardContext context) {
		return context != null && context.getMatch() != null;
	}
}
