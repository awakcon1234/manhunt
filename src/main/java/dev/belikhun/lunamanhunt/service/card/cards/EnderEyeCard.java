package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;

public class EnderEyeCard extends BaseCard {
	public EnderEyeCard() {
		super("ender_eye", "Mắt Ender", CardType.POWER_UP, CardRarity.COMMON, 1,
			"Nhận 1 mắt Ender.", Material.ENDER_EYE);
	}

	@Override
	public boolean apply(CardContext context) {
		context.giveItem(Material.ENDER_EYE, 1);
		return true;
	}
}
