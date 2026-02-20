package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public class AdrenalineCard extends BaseCard {
	public AdrenalineCard() {
		super("adrenaline", "Adrenaline", CardType.POWER_UP, CardRarity.RARE, 2,
			"Khi thấp máu sẽ nhận buff ngắn: sức mạnh, tốc độ và đào nhanh.", Material.RABBIT_FOOT, 90);
	}

	@Override
	public boolean apply(CardContext context) {
		return context != null && context.getMatch() != null;
	}
}
