package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HulkCard extends BaseCard {
	public HulkCard() {
		super("hulk", "Người khổng lồ xanh", CardType.EFFECT, CardRarity.UNCOMMON, 2,
			"Kháng sát thương II trong 1 phút.", Material.SLIME_BLOCK);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToSpeedrunner(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 60, 1));
	}
}
