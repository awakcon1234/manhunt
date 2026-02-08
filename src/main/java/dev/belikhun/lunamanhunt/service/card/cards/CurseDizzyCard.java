package dev.belikhun.lunamanhunt.service.card.cards;

import dev.belikhun.lunamanhunt.model.card.BaseCard;
import dev.belikhun.lunamanhunt.model.card.CardRarity;
import dev.belikhun.lunamanhunt.model.card.CardType;
import dev.belikhun.lunamanhunt.service.card.CardContext;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class CurseDizzyCard extends BaseCard {
	public CurseDizzyCard() {
		super("dizzy", "Hơi chóng mặt", CardType.CURSE, CardRarity.COMMON, 1,
			"Chóng mặt thợ săn trong 30 giây.", Material.FERMENTED_SPIDER_EYE, 30);
	}

	@Override
	public boolean apply(CardContext context) {
		return context.applyPotionToHunters(new PotionEffect(PotionEffectType.NAUSEA, 20 * 30, 0));
	}

	@Override
	public boolean clear(CardContext context) {
		return context.clearPotionFromHunters(PotionEffectType.NAUSEA);
	}
}
